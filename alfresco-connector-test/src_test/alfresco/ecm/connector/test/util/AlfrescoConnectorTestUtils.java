package alfresco.ecm.connector.test.util;

import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import ch.ivyteam.ivy.cm.ContentObjectValue;
import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.scripting.objects.File;

public class AlfrescoConnectorTestUtils {
  public static java.io.File exportFromCMS(String cmsUri, String ext) throws IOException {
    String file = StringUtils.removeStart(cmsUri, "/") + "." + ext;
    java.io.File tempFile = new File(file, true).getJavaFile();
    tempFile.getParentFile().mkdirs();
    ContentObjectValue cov = Ivy.cms().root().child().file(cmsUri, ext).value().get();
    try (var in = cov.read().inputStream(); var fos = new FileOutputStream(tempFile)) {
      IOUtils.copy(in, fos);
    }
    return tempFile;
  }
}
