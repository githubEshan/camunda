/*
 * Copyright © 2017 camunda services GmbH (info@camunda.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.camunda.spring.client.properties;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.util.unit.DataSize;

@ContextConfiguration(classes = CamundaClientPropertiesTestConfig.class)
public class CamundaClientPropertiesPostProcessorTest {

  @Autowired private CamundaClientProperties camundaClientProperties;

  @SpringBootTest(
      properties = {
        "zeebe.client.broker.grpc-address=http://legacy:26500",
        "camunda.client.zeebe.grpc-address=http://newer:26500",
        "zeebe.client.requestTimeout=PT1M",
        "camunda.client.tenant-ids=<default>, another one"
      })
  @Nested
  class BehaviourTest {
    @Autowired CamundaClientProperties camundaClientProperties;

    @Test
    void shouldPreferNewerProperties() {
      assertThat(camundaClientProperties.getGrpcAddress())
          .isEqualTo(URI.create("http://newer:26500"));
    }

    @Test
    void shouldUseRelaxedPropertyBinding() {
      assertThat(camundaClientProperties.getRequestTimeout()).isEqualTo(Duration.ofSeconds(60));
    }

    @Test
    void shouldMapLists() {
      assertThat(camundaClientProperties.getWorker().getDefaults().getTenantIds())
          .contains("<default>", "another one");
    }
  }

  @SpringBootTest(
      properties = {
        "zeebe.client.worker.override.foo.name=bar",
        "camunda.client.worker.override.custom.max-jobs-active=10",
        "zeebe.client.worker.override.custom.max-jobs-active=8",
        "camunda.client.worker.override.third.stream-enabled=true",
        "camunda.client.zeebe.override.third.stream-enabled=false"
      })
  @Nested
  class OverrideMappingTest {
    @Autowired CamundaClientProperties camundaClientProperties;

    @Test
    void shouldMapLegacyProperties() {
      assertThat(camundaClientProperties.getWorker().getOverride().get("foo").getName())
          .isEqualTo("bar");
    }

    @Test
    void shouldPreferNewerProperties() {
      assertThat(camundaClientProperties.getWorker().getOverride().get("custom").getMaxJobsActive())
          .isEqualTo(10);
      assertThat(camundaClientProperties.getWorker().getOverride().get("third").getStreamEnabled())
          .isTrue();
    }
  }

  @SpringBootTest(properties = {"ZEEBE_CLIENT_WORKER_OVERRIDE_CUSTOM_MAXJOBSACTIVE=8"})
  @Nested
  class EnvironmentVariablesWorkerOverrideTest {
    @Autowired CamundaClientProperties camundaClientProperties;

    @Test
    void shouldMapEnvironmentVariables() {
      assertThat(camundaClientProperties.getWorker().getOverride().get("custom").getMaxJobsActive())
          .isEqualTo(8);
    }
  }

  @Nested
  class CompatibilityTest {
    @Nested
    class _8_5 {

      @SpringBootTest(
          properties = {
            "zeebe.client.cloud.clusterId=xxx1",
            "zeebe.client.cloud.clientId=xxx2",
            "zeebe.client.cloud.clientSecret=xxx3",
            "zeebe.client.cloud.region=bru-2"
          })
      @Nested
      class CloudProperties {
        @Autowired CamundaClientProperties camundaClientProperties;

        @Test
        void shouldReadClientId() {
          assertThat(camundaClientProperties.getAuth().getClientId()).isEqualTo("xxx2");
        }

        @Test
        void shouldReadClientSecret() {
          assertThat(camundaClientProperties.getAuth().getClientSecret()).isEqualTo("xxx3");
        }

        @Test
        void shouldReadClientRestAddress() {
          assertThat(camundaClientProperties.getRestAddress())
              .isEqualTo(URI.create("https://bru-2.zeebe.camunda.io:443/xxx1"));
        }

        @Test
        void shouldReadClientGrpcAddress() {
          assertThat(camundaClientProperties.getGrpcAddress())
              .isEqualTo(URI.create("https://xxx1.bru-2.zeebe.camunda.io:443"));
        }

        @Test
        void shouldConfigureIssuer() {
          assertThat(camundaClientProperties.getAuth().getIssuer())
              .isEqualTo(URI.create("https://login.cloud.camunda.io/oauth/token"));
        }
      }

      @SpringBootTest(
          properties = {
            "zeebe.client.cloud.clientId=your-client-id",
            "zeebe.client.cloud.clientSecret=your-client-secret",
            "zeebe.client.cloud.authUrl=http://localhost:18081/auth/realms/your-realm/protocol/openid-connect/token",
            "zeebe.client.broker.grpcAddress=http://localhost:26500",
            "zeebe.client.broker.restAddress=http://localhost:8080",
            "zeebe.client.security.plaintext=true"
          })
      @Nested
      class SelfManagedProperties {
        @Autowired CamundaClientProperties camundaClientProperties;

        @Test
        void shouldReadClientId() {
          assertThat(camundaClientProperties.getAuth().getClientId()).isEqualTo("your-client-id");
        }

        @Test
        void shouldReadClientSecret() {
          assertThat(camundaClientProperties.getAuth().getClientSecret())
              .isEqualTo("your-client-secret");
        }

        @Test
        void shouldReadClientRestAddress() {
          assertThat(camundaClientProperties.getRestAddress())
              .isEqualTo(URI.create("http://localhost:8080"));
        }

        @Test
        void shouldReadClientGrpcAddress() {
          assertThat(camundaClientProperties.getGrpcAddress())
              .isEqualTo(URI.create("http://localhost:26500"));
        }

        @Test
        void shouldReadIssuer() {
          assertThat(camundaClientProperties.getAuth().getIssuer())
              .isEqualTo(
                  URI.create(
                      "http://localhost:18081/auth/realms/your-realm/protocol/openid-connect/token"));
        }
      }

      @SpringBootTest(properties = {"zeebe.client.worker.default-type=foo"})
      @Nested
      class DefaultJobType {
        @Autowired CamundaClientProperties camundaClientProperties;

        @Test
        void shouldReadDefaultType() {
          assertThat(camundaClientProperties.getWorker().getDefaults().getType()).isEqualTo("foo");
        }
      }

      @SpringBootTest(properties = {"camunda.client.zeebe.defaults.auto-complete: false"})
      @Nested
      class GlobalAutoComplete {
        @Autowired CamundaClientProperties camundaClientProperties;

        @Test
        void shouldReadGlobalAutoComplete() {
          assertThat(camundaClientProperties.getWorker().getDefaults().getAutoComplete()).isFalse();
        }
      }

      @SpringBootTest(properties = {"camunda.client.zeebe.override.foo.auto-complete: false"})
      @Nested
      class FooWorkerAutoComplete {
        @Autowired CamundaClientProperties camundaClientProperties;

        @Test
        void shouldReadGlobalAutoComplete() {
          assertThat(camundaClientProperties.getWorker().getOverride().get("foo").getAutoComplete())
              .isFalse();
        }
      }

      @SpringBootTest(
          properties = {
            "zeebe.client.cloud.cluster-id=123",
            "zeebe.client.cloud.base-url=zeebe.camundatest.io",
            "zeebe.client.cloud.port=1443",
            "zeebe.client.cloud.auth-url=https://login.cloud.camundatest.io/oauth/token"
          })
      @Nested
      class DifferentCloudEnvironment {

        @Autowired CamundaClientProperties camundaClientProperties;

        @Test
        void shouldReadClientRestAddress() {
          assertThat(camundaClientProperties.getRestAddress())
              .isEqualTo(URI.create("https://bru-2.zeebe.camundatest.io:1443/123"));
        }

        @Test
        void shouldReadClientGrpcAddress() {
          assertThat(camundaClientProperties.getGrpcAddress())
              .isEqualTo(URI.create("https://123.bru-2.zeebe.camundatest.io:1443"));
        }

        @Test
        void shouldReadIssuer() {
          assertThat(camundaClientProperties.getAuth().getIssuer())
              .isEqualTo(URI.create("https://login.cloud.camundatest.io/oauth/token"));
        }
      }

      @SpringBootTest(properties = {"zeebe.client.worker.defaultType=foo"})
      @Nested
      class DefaultTaskType {
        @Autowired CamundaClientProperties camundaClientProperties;

        @Test
        void shouldReadDefaultType() {
          assertThat(camundaClientProperties.getWorker().getDefaults().getType()).isEqualTo("foo");
        }
      }

      @SpringBootTest(
          properties = {"zeebe.client.worker.max-jobs-active=32", "zeebe.client.worker.threads=1"})
      @Nested
      class JobsInFlightAndThreadPool {
        @Autowired CamundaClientProperties camundaClientProperties;

        @Test
        void shouldReadMaxJobsActive() {
          assertThat(camundaClientProperties.getWorker().getDefaults().getMaxJobsActive())
              .isEqualTo(32);
        }

        @Test
        void shouldReadThreads() {
          assertThat(camundaClientProperties.getExecutionThreads()).isEqualTo(1);
        }
      }

      @SpringBootTest({"zeebe.client.worker.override.foo.enabled=false"})
      @Nested
      class DisableWorker {
        @Autowired CamundaClientProperties camundaClientProperties;

        @Test
        void shouldReadFooEnabled() {
          assertThat(camundaClientProperties.getWorker().getOverride().get("foo").getEnabled())
              .isFalse();
        }
      }

      @SpringBootTest({"zeebe.client.default-job-worker-stream-enabled=true"})
      @Nested
      class EnableJobWorkerStream {
        @Autowired CamundaClientProperties camundaClientProperties;

        @Test
        void shouldReadDefaultJobWorkerStream() {
          assertThat(camundaClientProperties.getWorker().getDefaults().getStreamEnabled()).isTrue();
        }
      }

      @SpringBootTest({"zeebe.client.default-job-worker-tenant-ids=myTenant"})
      @Nested
      class DefaultJobWorkerTenantIds {
        @Autowired CamundaClientProperties camundaClientProperties;

        @Test
        void shouldReadDefaultJobWorkerTenantIds() {
          assertThat(camundaClientProperties.getWorker().getDefaults().getTenantIds())
              .containsExactly("myTenant");
        }
      }

      @SpringBootTest({"zeebe.client.worker.override.foo.tenant-ids=myThirdTenant"})
      @Nested
      class OverrideJobWorkerTenantIds {
        @Autowired CamundaClientProperties camundaClientProperties;

        @Test
        void shouldReadOverrideJobWorkerTenantIds() {
          assertThat(camundaClientProperties.getWorker().getOverride().get("foo").getTenantIds())
              .containsExactly("myThirdTenant");
        }
      }

      @SpringBootTest({"zeebe.client.security.overrideAuthority=host:port"})
      @Nested
      class OverrideAuthority {
        @Autowired CamundaClientProperties camundaClientProperties;

        @Test
        void shouldReadOverrideAuthority() {
          assertThat(camundaClientProperties.getOverrideAuthority()).isEqualTo("host:port");
        }
      }

      @SpringBootTest({"zeebe.client.security.certPath=/path/to/cert.pem"})
      @Nested
      class CertPath {
        @Autowired CamundaClientProperties camundaClientProperties;

        @Test
        void shouldReadCertPath() {
          assertThat(camundaClientProperties.getCaCertificatePath()).isEqualTo("/path/to/cert.pem");
        }
      }

      @SpringBootTest({"zeebe.client.message.timeToLive=PT2H"})
      @Nested
      class MessageTimeToLive {
        @Autowired CamundaClientProperties camundaClientProperties;

        @Test
        void shouldReadMessageTimeToLive() {
          assertThat(camundaClientProperties.getMessageTimeToLive()).isEqualTo(Duration.ofHours(2));
        }
      }

      @SpringBootTest({"zeebe.client.message.maxMessage-size=3145728"})
      @Nested
      class MessageSize {
        @Autowired CamundaClientProperties camundaClientProperties;

        @Test
        void shouldReadMessageSize() {
          assertThat(camundaClientProperties.getMaxMessageSize())
              .isEqualTo(DataSize.parse("3145728"));
        }
      }

      @SpringBootTest({"zeebe.client.broker.keepAlive=PT60S"})
      @Nested
      class KeepAlive {
        @Autowired CamundaClientProperties camundaClientProperties;

        @Test
        void shouldReadKeepAlive() {
          assertThat(camundaClientProperties.getKeepAlive()).isEqualTo(Duration.ofSeconds(60));
        }
      }
    }

    @Nested
    class _8_6 {

      @Nested
      @SpringBootTest(
          properties = {
            "camunda.client.mode=saas",
            "camunda.client.auth.client-id=client-id",
            "camunda.client.auth.client-secret=client-secret",
            "camunda.client.cluster-id=12345",
            "camunda.client.region=abc-2"
          })
      class Saas {
        @Autowired CamundaClientProperties camundaClientProperties;

        @Test
        void shouldReadClientId() {
          assertThat(camundaClientProperties.getAuth().getClientId()).isEqualTo("client-id");
        }

        @Test
        void shouldReadClientSecret() {
          assertThat(camundaClientProperties.getAuth().getClientSecret())
              .isEqualTo("client-secret");
        }

        @Test
        void shouldReadClientRestAddress() {
          assertThat(camundaClientProperties.getRestAddress())
              .isEqualTo(URI.create("https://abc-2.zeebe.camunda.io:443/12345"));
        }

        @Test
        void shouldReadClientGrpcAddress() {
          assertThat(camundaClientProperties.getGrpcAddress())
              .isEqualTo(URI.create("https://12345.abc-2.zeebe.camunda.io:443"));
        }

        @Test
        void shouldConfigureIssuer() {
          assertThat(camundaClientProperties.getAuth().getIssuer())
              .isEqualTo(URI.create("https://login.cloud.camunda.io/oauth/token"));
        }
      }

      @Nested
      @SpringBootTest(
          properties = {
            "camunda.client.mode=self-managed",
            "camunda.client.auth.client-id=client-id",
            "camunda.client.auth.client-secret=client-secret",
            "camunda.client.auth.issuer=http://localhost:18081/auth/realms/camunda-platform/protocol/openid-connect/token"
          })
      class SelfManaged {
        @Autowired CamundaClientProperties camundaClientProperties;

        @Test
        void shouldReadClientId() {
          assertThat(camundaClientProperties.getAuth().getClientId()).isEqualTo("client-id");
        }

        @Test
        void shouldReadClientSecret() {
          assertThat(camundaClientProperties.getAuth().getClientSecret())
              .isEqualTo("client-secret");
        }

        @Test
        void shouldReadIssuer() {
          assertThat(camundaClientProperties.getAuth().getIssuer())
              .isEqualTo(
                  URI.create(
                      "http://localhost:18081/auth/realms/camunda-platform/protocol/openid-connect/token"));
        }
      }

      @Nested
      @SpringBootTest({
        "camunda.client.mode=self-managed",
        "camunda.client.tenant-ids=not-default",
        "camunda.client.auth.client-id=client-id",
        "camunda.client.auth.client-secret=client-secret",
        "camunda.client.auth.issuer=http://localhost:18081/auth/realms/camunda-platform/protocol/openid-connect/token",
        "camunda.client.zeebe.enabled=false",
        "camunda.client.zeebe.grpc-address=http://localhostaaa:26500",
        "camunda.client.zeebe.rest-address=http://localhostaaa:8080",
        "camunda.client.zeebe.prefer-rest-over-grpc=true",
        "camunda.client.zeebe.audience=zeebe-api-lulu",
        "camunda.client.zeebe.scope=scope"
      })
      class SelfManagedExtended {
        @Autowired CamundaClientProperties camundaClientProperties;

        @Test
        void shouldReadTenantIds() {
          assertThat(camundaClientProperties.getWorker().getDefaults().getTenantIds())
              .containsExactly("not-default");
        }

        @Test
        void shouldReadClientId() {
          assertThat(camundaClientProperties.getAuth().getClientId()).isEqualTo("client-id");
        }

        @Test
        void shouldReadClientSecret() {
          assertThat(camundaClientProperties.getAuth().getClientSecret())
              .isEqualTo("client-secret");
        }

        @Test
        void shouldReadIssuer() {
          assertThat(camundaClientProperties.getAuth().getIssuer())
              .isEqualTo(
                  URI.create(
                      "http://localhost:18081/auth/realms/camunda-platform/protocol/openid-connect/token"));
        }

        @Test
        void shouldReadZeebeEnabled() {
          assertThat(camundaClientProperties.getZeebe().getEnabled()).isFalse();
        }

        @Test
        void shouldReadZeebeGrpcAddress() {
          assertThat(camundaClientProperties.getGrpcAddress())
              .isEqualTo(URI.create("http://localhostaaa:26500"));
        }

        @Test
        void shouldReadZeebeRestAddress() {
          assertThat(camundaClientProperties.getRestAddress())
              .isEqualTo(URI.create("http://localhostaaa:8080"));
        }

        @Test
        void shouldReadPreferRestOverGrpc() {
          assertThat(camundaClientProperties.getPreferRestOverGrpc()).isTrue();
        }

        @Test
        void shouldReadAudience() {
          assertThat(camundaClientProperties.getAuth().getAudience()).isEqualTo("zeebe-api-lulu");
        }

        @Test
        void shouldReadScope() {
          assertThat(camundaClientProperties.getAuth().getScope()).isEqualTo("scope");
        }
      }
    }
  }
  // TODO add more tests to verify that all properties are mapped accordingly

}
