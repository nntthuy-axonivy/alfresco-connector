package alfresco.ecm.connector.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.alfresco.api.explorer.NodeEntry;

import alfresco.ecm.connector.test.util.AlfrescoConnectorTestUtils;
import ch.ivyteam.ivy.bpm.engine.client.BpmClient;
import ch.ivyteam.ivy.bpm.engine.client.element.BpmProcess;
import ch.ivyteam.ivy.bpm.engine.client.sub.SubProcessCallResult;
import ch.ivyteam.ivy.bpm.exec.client.IvyProcessTest;
import ch.ivyteam.ivy.environment.AppFixture;
import ch.ivyteam.ivy.scripting.objects.List;
import ch.ivyteam.ivy.security.ISession;

@Testcontainers
@IvyProcessTest
public class TestAlfrescoECM {
  private static final String FINISHED_SETUP_LOG_TEXT_REGEX = ".*readyProbe: Success - Tested.*";
  private static final String GET_DOCUMENTS_START_NAME = "getDocumentsFromFolder(String)";
  private static final String POST_DOCUMENTS_START_NAME = "postDocumentToFolder(String,File)";
  private static final String ADD_NEW_NODE_START_NAME = "addNewNodeFolder(String)";
  private static final String SHARE_FOLDER_NAME = "-shared-";
  private static final String DOCKER_COMPPOSE_FILE_PATH = "../alfresco-connector-demo/docker/compose.yaml";

  private static final BpmProcess CALL_READ_DOCUMENTS = BpmProcess.path("Alfresco/Documents");

  @BeforeEach
  void setup(AppFixture fixture) {
    // Default credential value from alfresco docker
    fixture.var("Alfresco.Username", "admin");
    fixture.var("Alfresco.Password", "admin");
  }

  /**
   * This Docker Compose file was cloned from Alfresco's official GitHub
   * repository:
   * https://github.com/Alfresco/acs-deployment/blob/master/docker-compose/community-compose.yaml
   *
   * Please ensure that you regularly update this file to align with the latest
   * version from the upstream repository.
   */
  @SuppressWarnings("resource")
  @Container
  public final ComposeContainer db2 = new ComposeContainer(new java.io.File(DOCKER_COMPPOSE_FILE_PATH))
      .withExposedService("alfresco", 8080).waitingFor("alfresco",
          Wait.forLogMessage(FINISHED_SETUP_LOG_TEXT_REGEX, 1).withStartupTimeout(Duration.ofMinutes(2)));

  @Test
  @SuppressWarnings("unchecked")
  public void callReadDocuments(BpmClient bpmClient, ISession session) throws IOException {
    SubProcessCallResult result = bpmClient.start()
      .subProcess(CALL_READ_DOCUMENTS.elementName(GET_DOCUMENTS_START_NAME)).as().session(session)
      .execute(SHARE_FOLDER_NAME).subResult();
    assertThat(result.param("documents", List.class)).isEmpty();
    bpmClient.start().subProcess(CALL_READ_DOCUMENTS.elementName(POST_DOCUMENTS_START_NAME)).as().session(session)
      .execute(SHARE_FOLDER_NAME, AlfrescoConnectorTestUtils.exportFromCMS("/Files/test", "yaml"));
    result = bpmClient.start().subProcess(CALL_READ_DOCUMENTS.elementName(GET_DOCUMENTS_START_NAME)).as()
      .session(session).execute(SHARE_FOLDER_NAME).subResult();
    assertThat(result.param("documents", List.class)).isNotEmpty();
    result = bpmClient.start().subProcess(CALL_READ_DOCUMENTS.elementName(ADD_NEW_NODE_START_NAME)).as()
      .session(session).execute("test").subResult();
    assertThat(result.param("nodeEntry", NodeEntry.class)).isNotNull();
  }
}