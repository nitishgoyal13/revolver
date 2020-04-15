package io.dropwizard.revolver.provider;

import lombok.*;

@Getter
@Builder
@ToString
@AllArgsConstructor
@EqualsAndHashCode
public class BlacklistMethodData {
    private final String httpMethod;
    private final String relativePath; // path of the method excluding parent resource path
    private final String resourceClassName;
}
