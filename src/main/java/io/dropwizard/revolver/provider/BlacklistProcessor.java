package io.dropwizard.revolver.provider;

import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.server.model.ModelProcessor;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.Resource.Builder;
import org.glassfish.jersey.server.model.ResourceMethod;
import org.glassfish.jersey.server.model.ResourceModel;

import javax.ws.rs.core.Configuration;
import javax.ws.rs.ext.Provider;
import java.util.List;
import java.util.Set;

@Provider
@Slf4j
public class BlacklistProcessor implements ModelProcessor {

    private final Set<BlacklistMethodData> blacklistMethods;

    public BlacklistProcessor(final Set<BlacklistMethodData> blacklistMethods) {
        this.blacklistMethods = blacklistMethods;
    }

    @Override
    public ResourceModel processResourceModel(final ResourceModel resourceModel, final Configuration configuration) {
        if (blacklistMethods == null || blacklistMethods.isEmpty()) {
            log.info("No API end-point to blacklist");
            return resourceModel;
        }

        ResourceModel.Builder newResourceModelBuilder = new ResourceModel.Builder(false);
        for (final Resource resource : resourceModel.getResources()) {
            final Resource.Builder resourceBuilder;

            final List<Resource> childResources = resource.getChildResources();

            resourceBuilder = childResources.isEmpty()
                    ? Resource.builder(resource)
                    : Resource.builder(resource.getPath());

            // Add child resources with non-blacklisted child resource methods only
            childResources.forEach(childResource -> {
                    final Builder childResourceBuilder = Resource.builder(childResource.getPath());
                    childResource.getResourceMethods().stream()
                            .filter(this::shouldAddMethod)
                            .forEach(childResourceBuilder::addMethod);
                    resourceBuilder.addChildResource(childResourceBuilder.build());
            });

            newResourceModelBuilder.addResource(resourceBuilder.build());
        }

        return newResourceModelBuilder.build();
    }

    @Override
    public ResourceModel processSubResource(final ResourceModel subResourceModel, final Configuration configuration) {
        return subResourceModel;
    }

    private boolean shouldAddMethod(final ResourceMethod method) {
        // This is slightly sub-optimal,
        // but given that blacklist count will be small, not creating a complex Map to match Jersey resource structure
        return blacklistMethods.stream()
                .noneMatch(blacklistData -> {
                    if (method.getInvocable().getHandler().getHandlerClass().getName().equals(blacklistData.getResourceClassName()) &&
                            method.getHttpMethod().equalsIgnoreCase(blacklistData.getHttpMethod()) &&
                            method.getParent().getPath().equals(blacklistData.getRelativePath())) {
                        log.info("Blacklisting method with path: {}, http method: {}, resource: {}",
                                blacklistData.getRelativePath(),
                                blacklistData.getHttpMethod(),
                                blacklistData.getResourceClassName());
                        return true;
                    }
                    return false;
                });
    }
}
