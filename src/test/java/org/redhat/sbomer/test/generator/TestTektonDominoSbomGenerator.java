/**
 * JBoss, Home of Professional Open Source.
 * Copyright 2023 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.redhat.sbomer.test.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.redhat.sbomer.generator.SbomGenerator;
import org.redhat.sbomer.generator.TektonDominoSbomGenerator;

import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.tekton.client.TektonClient;
import io.fabric8.tekton.client.dsl.V1beta1APIGroupDSL;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

@QuarkusTest
public class TestTektonDominoSbomGenerator {
    @Any
    @Inject
    Instance<SbomGenerator> generators;

    SbomGenerator generator;

    @InjectMock(convertScopes = true)
    TektonClient tektonClient;

    @BeforeEach
    void init() {
        generator = generators.select(TektonDominoSbomGenerator.class).get();
    }

    @Test
    void testDefaultDominoArguments() {
        var v1beta1 = Mockito.mock(V1beta1APIGroupDSL.class);
        var taskRuns = Mockito.mock(MixedOperation.class);
        var resource = Mockito.mock(Resource.class);

        Mockito.when(taskRuns.resource(any())).thenReturn(resource);
        Mockito.when(v1beta1.taskRuns()).thenReturn(taskRuns);
        Mockito.when(tektonClient.v1beta1()).thenReturn(v1beta1);

        generator.generate("AAABBBB");

        Mockito.verify(tektonClient.v1beta1().taskRuns(), Mockito.times(1)).resource(argThat(taskRun -> {
            assertNotNull(taskRun);
            assertEquals("sbomer-domino-aaabbbb-", taskRun.getMetadata().getGenerateName());
            assertEquals("sbomer-generate-domino", taskRun.getSpec().getTaskRef().getName());
            assertEquals("sbomer-sa", taskRun.getSpec().getServiceAccountName());
            assertEquals("AAABBBB", taskRun.getSpec().getParams().get(0).getValue().getStringVal());
            assertEquals(
                    "{\"additional-args\":\"--include-non-managed\"}",
                    taskRun.getSpec().getParams().get(1).getValue().getStringVal());
            assertEquals(1, taskRun.getSpec().getWorkspaces().size());
            assertEquals("data", taskRun.getSpec().getWorkspaces().get(0).getName());
            return true;
        }));
    }
}
