// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2012 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.server.project.youngandroid;

import com.google.appengine.api.utils.SystemProperty;
import com.google.apphosting.api.ApiProxy;
import com.google.appinventor.common.utils.StringUtils;
import com.google.appinventor.common.version.GitBuildId;
import com.google.appinventor.components.common.YaVersion;
import com.google.appinventor.server.CrashReport;
import com.google.appinventor.server.FileExporter;
import com.google.appinventor.server.FileExporterImpl;
import com.google.appinventor.server.Server;
import com.google.appinventor.server.encryption.EncryptionException;
import com.google.appinventor.server.flags.Flag;
import com.google.appinventor.server.project.CommonProjectService;
import com.google.appinventor.server.project.utils.Security;
import com.google.appinventor.server.properties.json.ServerJsonParser;
import com.google.appinventor.server.storage.StorageIo;
import com.google.appinventor.shared.properties.json.JSONParser;
import com.google.appinventor.shared.rpc.RpcResult;
import com.google.appinventor.shared.rpc.ServerLayout;
import com.google.appinventor.shared.rpc.project.NewProjectParameters;
import com.google.appinventor.shared.rpc.project.Project;
import com.google.appinventor.shared.rpc.project.ProjectNode;
import com.google.appinventor.shared.rpc.project.ProjectRootNode;
import com.google.appinventor.shared.rpc.project.ProjectSourceZip;
import com.google.appinventor.shared.rpc.project.RawFile;
import com.google.appinventor.shared.rpc.project.TextFile;
import com.google.appinventor.shared.rpc.project.youngandroid.NewYoungAndroidProjectParameters;
import com.google.appinventor.shared.rpc.project.youngandroid.YoungAndroidAssetNode;
import com.google.appinventor.shared.rpc.project.youngandroid.YoungAndroidAssetsFolder;
import com.google.appinventor.shared.rpc.project.youngandroid.YoungAndroidBlocksNode;
import com.google.appinventor.shared.rpc.project.youngandroid.YoungAndroidFormNode;
import com.google.appinventor.shared.rpc.project.youngandroid.YoungAndroidPackageNode;
import com.google.appinventor.shared.rpc.project.youngandroid.YoungAndroidProjectNode;
import com.google.appinventor.shared.rpc.project.youngandroid.YoungAndroidSourceFolderNode;
import com.google.appinventor.shared.rpc.project.youngandroid.YoungAndroidSourceNode;
import com.google.appinventor.shared.rpc.project.youngandroid.YoungAndroidYailNode;
import com.google.appinventor.shared.rpc.user.User;
import com.google.appinventor.shared.settings.Settings;
import com.google.appinventor.shared.settings.SettingsConstants;
import com.google.appinventor.shared.storage.StorageUtil;
import com.google.appinventor.shared.youngandroid.YoungAndroidSourceAnalyzer;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.common.io.CharStreams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.logging.Logger;

/**
 * Provides support for Young Android projects.
 *
 * @author lizlooney@google.com (Liz Looney)
 * @author markf@google.com (Mark Friedman)
 */
public final class YoungAndroidProjectService extends CommonProjectService {

  private static int currentProgress = 0;
  private static final Logger LOG = Logger.getLogger(YoungAndroidProjectService.class.getName());

  // The value of this flag can be changed in appengine-web.xml
  private static final Flag<Boolean> sendGitVersion =
    Flag.createFlag("build.send.git.version", true);

  // Project folder prefixes
  public static final String SRC_FOLDER = YoungAndroidSourceAnalyzer.SRC_FOLDER;
  protected static final String ASSETS_FOLDER = "assets";
  static final String PROJECT_DIRECTORY = "youngandroidproject";

  // TODO(user) Source these from a common constants library.
  private static final String FORM_PROPERTIES_EXTENSION =
      YoungAndroidSourceAnalyzer.FORM_PROPERTIES_EXTENSION;
  private static final String CODEBLOCKS_SOURCE_EXTENSION =
      YoungAndroidSourceAnalyzer.CODEBLOCKS_SOURCE_EXTENSION;
  private static final String BLOCKLY_SOURCE_EXTENSION =
      YoungAndroidSourceAnalyzer.BLOCKLY_SOURCE_EXTENSION;
  private static final String YAIL_FILE_EXTENSION =
      YoungAndroidSourceAnalyzer.YAIL_FILE_EXTENSION;

  public static final String PROJECT_PROPERTIES_FILE_NAME = PROJECT_DIRECTORY + "/" +
      "project.properties";

  private static final JSONParser JSON_PARSER = new ServerJsonParser();

  // Build folder path
  private static final String BUILD_FOLDER = "build";

  public static final String PROJECT_KEYSTORE_LOCATION = "android.keystore";

  // host[:port] to use for connecting to the build server
  private static final Flag<String> buildServerHost =
      Flag.createFlag("build.server.host", "localhost:9990");
  // host[:port] to tell build server app host url
  private static final Flag<String> appengineHost =
      Flag.createFlag("appengine.host", "");

  public YoungAndroidProjectService(StorageIo storageIo) {
    super(YoungAndroidProjectNode.YOUNG_ANDROID_PROJECT_TYPE, storageIo);
  }

  /**
   * Returns project settings that can be used when creating a new project.
   */
  public static String getProjectSettings(String icon, String vCode, String vName,
    String useslocation, String mapskey, String aName, String sizing) {
    icon = Strings.nullToEmpty(icon);
    vCode = Strings.nullToEmpty(vCode);
    vName = Strings.nullToEmpty(vName);
    useslocation = Strings.nullToEmpty(useslocation);
    mapskey = Strings.nullToEmpty(mapskey);
    sizing = Strings.nullToEmpty(sizing);
    aName = Strings.nullToEmpty(aName);
    return "{\"" + SettingsConstants.PROJECT_YOUNG_ANDROID_SETTINGS + "\":{" +
        "\"" + SettingsConstants.YOUNG_ANDROID_SETTINGS_ICON + "\":\"" + icon +
        "\",\"" + SettingsConstants.YOUNG_ANDROID_SETTINGS_VERSION_CODE + "\":\"" + vCode +
        "\",\"" + SettingsConstants.YOUNG_ANDROID_SETTINGS_VERSION_NAME + "\":\"" + vName +
        "\",\"" + SettingsConstants.YOUNG_ANDROID_SETTINGS_USES_LOCATION + "\":\"" + useslocation +
        "\",\"" + SettingsConstants.YOUNG_ANDROID_SETTINGS_MAPS_KEY + "\":\"" + mapskey +
        "\",\"" + SettingsConstants.YOUNG_ANDROID_SETTINGS_APP_NAME + "\":\"" + aName +
        "\",\"" + SettingsConstants.YOUNG_ANDROID_SETTINGS_SIZING + "\":\"" + sizing +
        "\"}}";
  }

  /**
   * Returns the contents of the project properties file for a new Young Android
   * project.
   *
   * @param projectName the name of the project
   * @param qualifiedName the qualified name of Screen1 in the project
   * @param icon the name of the asset to use as the application icon
   * @param vcode the version code
   * @param vname the version name
   * @param mapskey the key for the Google Maps API
   * @param aName the Application Name
   */
  public static String getProjectPropertiesFileContents(String projectName, String qualifiedName,
    String icon, String vcode, String vname, String useslocation, String mapskey, String aname, String sizing) {
    String contents = "main=" + qualifiedName + "\n" +
        "name=" + projectName + '\n' +
        "assets=../" + ASSETS_FOLDER + "\n" +
        "source=../" + SRC_FOLDER + "\n" +
        "build=../build\n";
    if (icon != null && !icon.isEmpty()) {
      contents += "icon=" + icon + "\n";
    }
    if (vcode != null && !vcode.isEmpty()) {
      contents += "versioncode=" + vcode + "\n";
    }
    if (vname != null && !vname.isEmpty()) {
      contents += "versionname=" + vname + "\n";
    }
    if (useslocation != null && !useslocation.isEmpty()) {
      contents += "useslocation=" + useslocation + "\n";
    }
    if (mapskey != null && !mapskey.isEmpty()) {
      contents += "mapskey=" + mapskey + "\n";
      }
    if (aname != null) {
      contents += "aname=" + aname + "\n";
    }
    if (sizing != null && !sizing.isEmpty()) {
      contents += "sizing=" + sizing + "\n";
    }
    return contents;
  }

  /**
   * Returns the contents of a new Young Android form file.
   * @param qualifiedName the qualified name of the form.
   * @return the contents of a new Young Android form file.
   */
  @VisibleForTesting
  public static String getInitialFormPropertiesFileContents(String qualifiedName) {
    final int lastDotPos = qualifiedName.lastIndexOf('.');
    String packageName = qualifiedName.split("\\.")[2];
    String formName = qualifiedName.substring(lastDotPos + 1);
    // The initial Uuid is set to zero here since (as far as we know) we can't get random numbers
    // in ode.shared.  This shouldn't actually matter since all Uuid's are random int's anyway (and
    // 0 was randomly chosen, I promise).  The TODO(user) in MockComponent.java indicates that
    // there will someday be assurance that these random Uuid's are unique.  Once that happens
    // this will be perfectly acceptable.  Until that happens, choosing 0 is just as safe as
    // allowing a random number to be chosen when the MockComponent is first created.
    return "#|\n$JSON\n" +
        "{\"YaVersion\":\"" + YaVersion.YOUNG_ANDROID_VERSION + "\",\"Source\":\"Form\"," +
        "\"Properties\":{\"$Name\":\"" + formName + "\",\"$Type\":\"Form\"," +
        "\"$Version\":\"" + YaVersion.FORM_COMPONENT_VERSION + "\",\"Uuid\":\"" + 0 + "\"," +
        "\"Title\":\"" + formName + "\",\"AppName\":\"" + packageName +"\"}}\n|#";
  }

  /**
   * Returns the initial contents of a Young Android blockly blocks file.
   */
  private static String getInitialBlocklySourceFileContents(String qualifiedName) {
    return "";
  }

  private static String packageNameToPath(String packageName) {
    return SRC_FOLDER + '/' + packageName.replace('.', '/');
  }

  public static String getSourceDirectory(String qualifiedName) {
    return StorageUtil.dirname(packageNameToPath(qualifiedName));
  }

  // CommonProjectService implementation

  @Override
  public void storeProjectSettings(String userId, long projectId, String projectSettings) {
    super.storeProjectSettings(userId, projectId, projectSettings);

    // If the icon has been changed, update the project properties file.
    // Extract the new icon from the projectSettings parameter.
    Settings settings = new Settings(JSON_PARSER, projectSettings);
    String newIcon = Strings.nullToEmpty(settings.getSetting(
        SettingsConstants.PROJECT_YOUNG_ANDROID_SETTINGS,
        SettingsConstants.YOUNG_ANDROID_SETTINGS_ICON));
    String newVCode = Strings.nullToEmpty(settings.getSetting(
        SettingsConstants.PROJECT_YOUNG_ANDROID_SETTINGS,
        SettingsConstants.YOUNG_ANDROID_SETTINGS_VERSION_CODE));
    String newVName = Strings.nullToEmpty(settings.getSetting(
        SettingsConstants.PROJECT_YOUNG_ANDROID_SETTINGS,
        SettingsConstants.YOUNG_ANDROID_SETTINGS_VERSION_NAME));
    String newUsesLocation = Strings.nullToEmpty(settings.getSetting(
        SettingsConstants.PROJECT_YOUNG_ANDROID_SETTINGS,
        SettingsConstants.YOUNG_ANDROID_SETTINGS_USES_LOCATION));
    String newMapsKey = Strings.nullToEmpty(settings.getSetting(
        SettingsConstants.PROJECT_YOUNG_ANDROID_SETTINGS,
        SettingsConstants.YOUNG_ANDROID_SETTINGS_MAPS_KEY));
    String newSizing = Strings.nullToEmpty(settings.getSetting(
        SettingsConstants.PROJECT_YOUNG_ANDROID_SETTINGS,
        SettingsConstants.YOUNG_ANDROID_SETTINGS_SIZING));
    String newAName = Strings.nullToEmpty(settings.getSetting(
        SettingsConstants.PROJECT_YOUNG_ANDROID_SETTINGS,
        SettingsConstants.YOUNG_ANDROID_SETTINGS_APP_NAME));

    // Extract the old icon from the project.properties file from storageIo.
    String projectProperties = storageIo.downloadFile(userId, projectId,
        PROJECT_PROPERTIES_FILE_NAME, StorageUtil.DEFAULT_CHARSET);
    Properties properties = new Properties();
    try {
      properties.load(new StringReader(projectProperties));
    } catch (IOException e) {
      // Since we are reading from a String, I don't think this exception can actually happen.
      e.printStackTrace();
      return;
    }
    String oldIcon = Strings.nullToEmpty(properties.getProperty("icon"));
    String oldVCode = Strings.nullToEmpty(properties.getProperty("versioncode"));
    String oldVName = Strings.nullToEmpty(properties.getProperty("versionname"));
    String oldUsesLocation = Strings.nullToEmpty(properties.getProperty("useslocation"));
    String oldMapsKey = Strings.nullToEmpty(properties.getProperty("mapskey"));
    String oldSizing = Strings.nullToEmpty(properties.getProperty("sizing"));
    String oldAName = Strings.nullToEmpty(properties.getProperty("aname"));

    if (!newIcon.equals(oldIcon) || !newVCode.equals(oldVCode) || !newVName.equals(oldVName)
      || !newUsesLocation.equals(oldUsesLocation) ||  !newMapsKey.equals(oldMapsKey)
      || !newAName.equals(oldAName) || !newSizing.equals(oldSizing)) {
      // Recreate the project.properties and upload it to storageIo.
      String projectName = properties.getProperty("name");
      String qualifiedName = properties.getProperty("main");
      String newContent = getProjectPropertiesFileContents(projectName, qualifiedName, newIcon,
        newVCode, newVName, newUsesLocation, newMapsKey, newAName, newSizing);
      storageIo.uploadFileForce(projectId, PROJECT_PROPERTIES_FILE_NAME, userId,
        newContent, StorageUtil.DEFAULT_CHARSET);
    }
  }

  /**
   * {@inheritDoc}
   *
   * {@code params} needs to be an instance of
   * {@link NewYoungAndroidProjectParameters}.
   */
  @Override
  public long newProject(String userId, String projectName, NewProjectParameters params) {
    NewYoungAndroidProjectParameters youngAndroidParams = (NewYoungAndroidProjectParameters) params;
    String qualifiedFormName = youngAndroidParams.getQualifiedFormName();

    String propertiesFileName = PROJECT_PROPERTIES_FILE_NAME;
    String propertiesFileContents = getProjectPropertiesFileContents(projectName,
      qualifiedFormName, null, null, null, null, null, null, null);

    String formFileName = YoungAndroidFormNode.getFormFileId(qualifiedFormName);
    String formFileContents = getInitialFormPropertiesFileContents(qualifiedFormName);

    String blocklyFileName = YoungAndroidBlocksNode.getBlocklyFileId(qualifiedFormName);
    String blocklyFileContents = getInitialBlocklySourceFileContents(qualifiedFormName);

    String yailFileName = YoungAndroidYailNode.getYailFileId(qualifiedFormName);
    String yailFileContents = "";

    Project project = new Project(projectName);
    project.setProjectType(YoungAndroidProjectNode.YOUNG_ANDROID_PROJECT_TYPE);
    // Project history not supported in legacy ode new project wizard
    project.addTextFile(new TextFile(propertiesFileName, propertiesFileContents));
    project.addTextFile(new TextFile(formFileName, formFileContents));
    project.addTextFile(new TextFile(blocklyFileName, blocklyFileContents));
    project.addTextFile(new TextFile(yailFileName, yailFileContents));

    // Create new project
    return storageIo.createProject(userId, project, getProjectSettings("", "1", "1.0", "false", "", projectName, "Fixed"));
  }

  @Override
  public long copyProject(String userId, long oldProjectId, String newName) {
    String oldName = storageIo.getProjectName(userId, oldProjectId);
    String oldProjectSettings = storageIo.loadProjectSettings(userId, oldProjectId);
    String oldProjectHistory = storageIo.getProjectHistory(userId, oldProjectId);
    Settings oldSettings = new Settings(JSON_PARSER, oldProjectSettings);
    String icon = oldSettings.getSetting(
        SettingsConstants.PROJECT_YOUNG_ANDROID_SETTINGS,
        SettingsConstants.YOUNG_ANDROID_SETTINGS_ICON);
    String vcode = oldSettings.getSetting(
        SettingsConstants.PROJECT_YOUNG_ANDROID_SETTINGS,
        SettingsConstants.YOUNG_ANDROID_SETTINGS_VERSION_CODE);
    String vname = oldSettings.getSetting(
        SettingsConstants.PROJECT_YOUNG_ANDROID_SETTINGS,
        SettingsConstants.YOUNG_ANDROID_SETTINGS_VERSION_NAME);
    String useslocation = oldSettings.getSetting(
        SettingsConstants.PROJECT_YOUNG_ANDROID_SETTINGS,
        SettingsConstants.YOUNG_ANDROID_SETTINGS_USES_LOCATION);
    String mapskey = oldSettings.getSetting(
        SettingsConstants.PROJECT_YOUNG_ANDROID_SETTINGS,
        SettingsConstants.YOUNG_ANDROID_SETTINGS_MAPS_KEY);
    String aname = oldSettings.getSetting(
        SettingsConstants.PROJECT_YOUNG_ANDROID_SETTINGS,
        SettingsConstants.YOUNG_ANDROID_SETTINGS_APP_NAME);
    String sizing = oldSettings.getSetting(
        SettingsConstants.PROJECT_YOUNG_ANDROID_SETTINGS,
        SettingsConstants.YOUNG_ANDROID_SETTINGS_SIZING);

    Project newProject = new Project(newName);
    newProject.setProjectType(YoungAndroidProjectNode.YOUNG_ANDROID_PROJECT_TYPE);
    newProject.setProjectHistory(oldProjectHistory);

    // Get the old project's source files and add them to new project, modifying where necessary.
    for (String oldSourceFileName : storageIo.getProjectSourceFiles(userId, oldProjectId)) {
      String newSourceFileName;

      String newContents = null;
      if (oldSourceFileName.equals(PROJECT_PROPERTIES_FILE_NAME)) {
        // This is the project properties file. The name of the file doesn't contain the old
        // project name.
        newSourceFileName = oldSourceFileName;
        // For the contents of the project properties file, generate the file with the new project
        // name and qualified name.
        String qualifiedFormName = StringUtils.getQualifiedFormName(
            storageIo.getUser(userId).getUserEmail(), newName);
        newContents = getProjectPropertiesFileContents(newName, qualifiedFormName, icon,
          vcode, vname, useslocation, mapskey, aname, sizing);
      } else {
        // This is some file other than the project properties file.
        // oldSourceFileName may contain the old project name as a path segment, surrounded by /.
        // Replace the old name with the new name.
        newSourceFileName = StringUtils.replaceLastOccurrence(oldSourceFileName,
            "/" + oldName + "/", "/" + newName + "/");
      }

      if (newContents != null) {
        // We've determined (above) that the contents of the file must change for the new project.
        // Use newContents when adding the file to the new project.
        newProject.addTextFile(new TextFile(newSourceFileName, newContents));
      } else {
        // If we get here, we know that the contents of the file can just be copied from the old
        // project. Since it might be a binary file, we copy it as a raw file (that works for both
        // text and binary files).
        byte[] contents = storageIo.downloadRawFile(userId, oldProjectId, oldSourceFileName);
        newProject.addRawFile(new RawFile(newSourceFileName, contents));
      }
    }

    // Create the new project and return the new project's id.
    return storageIo.createProject(userId, newProject, getProjectSettings(icon, vcode, vname, mapskey,
        useslocation, aname, sizing));
  }

  @Override
  public ProjectRootNode getRootNode(String userId, long projectId) {
    // Create root, assets, and source nodes (they are mocked nodes as they don't really
    // have to exist like this on the file system)
    ProjectRootNode rootNode =
        new YoungAndroidProjectNode(storageIo.getProjectName(userId, projectId),
                                    projectId);
    ProjectNode assetsNode = new YoungAndroidAssetsFolder(ASSETS_FOLDER);
    ProjectNode sourcesNode = new YoungAndroidSourceFolderNode(SRC_FOLDER);

    rootNode.addChild(assetsNode);
    rootNode.addChild(sourcesNode);

    // Sources contains nested folders that are interpreted as packages
    Map<String, ProjectNode> packagesMap = Maps.newHashMap();

    // Retrieve project information
    List<String> sourceFiles = storageIo.getProjectSourceFiles(userId, projectId);
    for (String fileId : sourceFiles) {
      if (fileId.startsWith(ASSETS_FOLDER + '/')) {
        // Assets is a flat folder
        assetsNode.addChild(new YoungAndroidAssetNode(StorageUtil.basename(fileId), fileId));

      } else if (fileId.startsWith(SRC_FOLDER + '/')) {
        // We send form (.scm), blocks (.blk), and yail (.yail) nodes to the ODE client.
        YoungAndroidSourceNode sourceNode = null;
        if (fileId.endsWith(FORM_PROPERTIES_EXTENSION)) {
          sourceNode = new YoungAndroidFormNode(fileId);
        } else if (fileId.endsWith(BLOCKLY_SOURCE_EXTENSION)) {
          sourceNode = new YoungAndroidBlocksNode(fileId);
        } else if (fileId.endsWith(CODEBLOCKS_SOURCE_EXTENSION)) {
          String blocklyFileName = 
              fileId.substring(0, fileId.lastIndexOf(CODEBLOCKS_SOURCE_EXTENSION)) 
              + BLOCKLY_SOURCE_EXTENSION;
          if (!sourceFiles.contains(blocklyFileName)) {
            // This is an old project that hasn't been converted yet. Convert
            // the blocks file to Blockly format and name. Leave the old
            // codeblocks file around for now (for debugging) but don't send it to the client.
            String blocklyFileContents = convertCodeblocksToBlockly(userId, projectId, fileId);
            storageIo.addSourceFilesToProject(userId, projectId, false, blocklyFileName);
            storageIo.uploadFileForce(projectId, blocklyFileName, userId, blocklyFileContents,
                StorageUtil.DEFAULT_CHARSET);
            sourceNode = new YoungAndroidBlocksNode(blocklyFileName);
          }
        } else if (fileId.endsWith(YAIL_FILE_EXTENSION)) {
          sourceNode = new YoungAndroidYailNode(fileId);
        }
        if (sourceNode != null) {
          String packageName = StorageUtil.getPackageName(sourceNode.getQualifiedName());
          ProjectNode packageNode = packagesMap.get(packageName);
          if (packageNode == null) {
            packageNode = new YoungAndroidPackageNode(packageName, packageNameToPath(packageName));
            packagesMap.put(packageName, packageNode);
            sourcesNode.addChild(packageNode);
          }
          packageNode.addChild(sourceNode);
        }
      }
    }

    return rootNode;
  }
  
  /*
   * Convert the contents of the codeblocks file named codeblocksFileId
   * to blockly format and return the blockly contents.
   */
  private String convertCodeblocksToBlockly(String userId, long projectId, 
      String codeblocksFileId) {
    // TODO(sharon): implement this!
    return "";
  }

  @Override
  public long addFile(String userId, long projectId, String fileId) {
    if (fileId.endsWith(FORM_PROPERTIES_EXTENSION) ||
        fileId.endsWith(BLOCKLY_SOURCE_EXTENSION)) {
      // If the file to be added is a form file or a blocks file, add a new form file, a new
      // blocks file, and a new yail file (as a placeholder for later code generation)
      String qualifiedFormName = YoungAndroidSourceNode.getQualifiedName(fileId);
      String formFileName = YoungAndroidFormNode.getFormFileId(qualifiedFormName);
      String blocklyFileName = YoungAndroidBlocksNode.getBlocklyFileId(qualifiedFormName);
      String yailFileName = YoungAndroidYailNode.getYailFileId(qualifiedFormName);

      List<String> sourceFiles = storageIo.getProjectSourceFiles(userId, projectId);
      if (!sourceFiles.contains(formFileName) &&
          !sourceFiles.contains(blocklyFileName) &&
          !sourceFiles.contains(yailFileName)) {

        String formFileContents = getInitialFormPropertiesFileContents(qualifiedFormName);
        storageIo.addSourceFilesToProject(userId, projectId, false, formFileName);
        storageIo.uploadFileForce(projectId, formFileName, userId, formFileContents,
            StorageUtil.DEFAULT_CHARSET);

        String blocklyFileContents = getInitialBlocklySourceFileContents(qualifiedFormName);
        storageIo.addSourceFilesToProject(userId, projectId, false, blocklyFileName);
        storageIo.uploadFileForce(projectId, blocklyFileName, userId, blocklyFileContents,
            StorageUtil.DEFAULT_CHARSET);

        String yailFileContents = "";  // start empty
        storageIo.addSourceFilesToProject(userId, projectId, false, yailFileName);
        return storageIo.uploadFileForce(projectId, yailFileName, userId, yailFileContents,
            StorageUtil.DEFAULT_CHARSET);
      } else {
        throw new IllegalStateException("One or more files to be added already exists.");
      }

    } else {
      return super.addFile(userId, projectId, fileId);
    }
  }
  
  @Override
  public long copyScreen(String userId, long projectId, String targetFileId, String fileId) {
    if (fileId.endsWith(FORM_PROPERTIES_EXTENSION) ||
        fileId.endsWith(BLOCKLY_SOURCE_EXTENSION)) {
      // If the file to be added is a form file or a blocks file, add a new form file, a new
      // blocks file, and a new yail file (as a placeholder for later code generation)
      String qualifiedFormName = YoungAndroidSourceNode.getQualifiedName(fileId);
      String formFileName = YoungAndroidFormNode.getFormFileId(qualifiedFormName);
      String blocklyFileName = YoungAndroidBlocksNode.getBlocklyFileId(qualifiedFormName);
      String yailFileName = YoungAndroidYailNode.getYailFileId(qualifiedFormName);
      
      String targetQualifiedFormName = YoungAndroidSourceNode.getQualifiedName(targetFileId);
      String targetFormFileName = YoungAndroidFormNode.getFormFileId(targetQualifiedFormName);
      String targetBlocklyFileName = YoungAndroidBlocksNode.getBlocklyFileId(targetQualifiedFormName);
      String targetYailFileName = YoungAndroidYailNode.getYailFileId(targetQualifiedFormName);

      List<String> sourceFiles = storageIo.getProjectSourceFiles(userId, projectId);
      if (!sourceFiles.contains(formFileName) &&
          !sourceFiles.contains(blocklyFileName) &&
          !sourceFiles.contains(yailFileName)) {

        if (sourceFiles.contains(targetFormFileName) &&
            sourceFiles.contains(targetBlocklyFileName) &&
            sourceFiles.contains(targetYailFileName)) {
          
          //Screen1, or Screen2
          int lastDotPos = qualifiedFormName.lastIndexOf('.');
          String simpleFormName = qualifiedFormName.substring(lastDotPos + 1);
          lastDotPos = targetQualifiedFormName.lastIndexOf('.');
          String simpleTargetFormName = targetQualifiedFormName.substring(lastDotPos + 1);
          
          String formFileContents = load(userId, projectId, targetFormFileName)
              .replace(simpleTargetFormName, simpleFormName);
          storageIo.addSourceFilesToProject(userId, projectId, false, formFileName);
          storageIo.uploadFileForce(projectId, formFileName, userId, formFileContents,
              StorageUtil.DEFAULT_CHARSET);

          String blocklyFileContents = load(userId, projectId, targetBlocklyFileName)
              .replace(simpleTargetFormName, simpleFormName);
          storageIo.addSourceFilesToProject(userId, projectId, false, blocklyFileName);
          storageIo.uploadFileForce(projectId, blocklyFileName, userId, blocklyFileContents,
              StorageUtil.DEFAULT_CHARSET);

          String yailFileContents = load(userId, projectId, targetYailFileName)
              .replace(simpleTargetFormName, simpleFormName);
          storageIo.addSourceFilesToProject(userId, projectId, false, yailFileName);
          return storageIo.uploadFileForce(projectId, yailFileName, userId, yailFileContents,
              StorageUtil.DEFAULT_CHARSET);
        } else {
          throw new IllegalStateException("One or more files to be copied don't exist.");
        }
      } else {
        throw new IllegalStateException("One or more files to be added already exists.");
      }
    } else {
      return super.addFile(userId, projectId, fileId);
    }
  }
  
  @Override
  public long addLDForm(String userId, long projectId, String targetFileId, List<String> uriCollection, String conceptURI) {
    if (targetFileId.endsWith(FORM_PROPERTIES_EXTENSION) ||
        targetFileId.endsWith(BLOCKLY_SOURCE_EXTENSION)) {
      // If the file to be added is a form file or a blocks file, add a new form file, a new
      // blocks file, and a new yail file (as a placeholder for later code generation)      
      String targetQualifiedFormName = YoungAndroidSourceNode.getQualifiedName(targetFileId);
      String targetFormFileName = YoungAndroidFormNode.getFormFileId(targetQualifiedFormName);
      String targetBlocklyFileName = YoungAndroidBlocksNode.getBlocklyFileId(targetQualifiedFormName);
      String targetYailFileName = YoungAndroidYailNode.getYailFileId(targetQualifiedFormName);

      List<String> sourceFiles = storageIo.getProjectSourceFiles(userId, projectId);
      if (sourceFiles.contains(targetFormFileName) &&
          sourceFiles.contains(targetBlocklyFileName) &&
          sourceFiles.contains(targetYailFileName)) {
          
          String formFileContents = load(userId, projectId, targetFormFileName);
          LOG.info("The original form file content is " + formFileContents);
          
          String newString = formFileContents.replace("#|", "");
          newString = newString.replace("$JSON", "");
          newString = newString.replace("|#", "");
          
          String LDName = "";
          String LDFieldName = "";
          
          try {
            JSONObject originalObj = new JSONObject(newString);
            JSONObject originalObj2 = (JSONObject) originalObj.get("Properties");
            
            
            JSONArray originalObj3;
            
            try {
              originalObj3 = (JSONArray) originalObj2.get("$Components");
            } catch (JSONException e) {
            	originalObj3 = new JSONArray();
            }

            List<String> returns = generateFormContent(conceptURI, uriCollection);
            String formContent = returns.get(0);
            LDFieldName = returns.get(1);
            LOG.info("The generateFormContent " + formContent);
            JSONObject formObj = new JSONObject(formContent);
            
            JSONArray tmpObj = (JSONArray) formObj.get("$Components");
            originalObj3.put(originalObj3.length(), tmpObj.get(0));
            
            returns = generateLDContent();
            String LDContent = returns.get(0);
            LDName = returns.get(1);
            LOG.info("The generateLDContent " + LDContent);
            formObj = new JSONObject(LDContent);
            
            tmpObj = (JSONArray) formObj.get("$Components");
            originalObj3.put(originalObj3.length(), tmpObj.get(0));
            
            originalObj2.put("$Components", originalObj3);
            originalObj.put("Properties", originalObj2);
            
            String result = "#|" + "\n" + "$JSON" + "\n" + originalObj.toString() + "\n" + "|#";
            LOG.info("The smaller form file content is " + result);
            LOG.info("The uri collection is " + uriCollection.toString());
            formFileContents = result;
          } catch (JSONException e) {
            LOG.info("The exception is "+e.toString());
          }
          
          long uploadResult = storageIo.uploadFileForce(projectId, targetFormFileName, userId, formFileContents,
              StorageUtil.DEFAULT_CHARSET);
          
          String blkFileContents = load(userId, projectId, targetBlocklyFileName);
          LOG.info("The original blk content is \n"+ blkFileContents);

          String[] lines = blkFileContents.split("\\r?\\n");
          String lastLine1 = lines[lines.length-1];
          String lastLine2 = lines[lines.length-2];
          List<String> allLinesPart1 = Arrays.asList(lines).subList(0, lines.length-2);

          String[] tempBlkContent = generateBlockContent("LinkedData"+LDName, "LinkedDataForm"+LDFieldName);
          List<String> allLinesPart2 = Arrays.asList(tempBlkContent);

          List<String> allLines = new ArrayList<String>();
          allLines.addAll(allLinesPart1);
          allLines.addAll(allLinesPart2);
          allLines.add(lastLine2);
          allLines.add(lastLine1);
          
          String finalBlkContent = allLines.toString();
          finalBlkContent = finalBlkContent.replace(",", "\n");
          finalBlkContent = finalBlkContent.replace("[", "");
          finalBlkContent = finalBlkContent.replace("]", "");
          
          LOG.info("The new blk content is \n"+ finalBlkContent);
          uploadResult = storageIo.uploadFileForce(projectId, targetBlocklyFileName, userId, finalBlkContent,
              StorageUtil.DEFAULT_CHARSET);
          
          return uploadResult;
      } else {
          throw new IllegalStateException("One or more files to be copied don't exist.");
      }
    } else {
      return super.addFile(userId, projectId, targetFileId);
    }
  }
  
  public List<String> generateLDContent() {
    String contentPart = 
    "{"+
      "\"$Components\": ["+
        "{"+
          "\"$Name\": \"LinkedData$LDID$\","+
          "\"$Type\": \"LinkedData\","+
          "\"$Version\": \"3\","+
          "\"Uuid\": \"$LDUUID$\""+
        "}"+
      "]"+
    "}";
    String LDIDRegex = "$LDID$";
    String LDUUIDRegex = "$LDUUID$";

    String LDID = generateRandomLetterOrNum();
    contentPart = contentPart.replace(LDIDRegex, LDID);
    contentPart = contentPart.replace(LDUUIDRegex, System.currentTimeMillis()+"");
    List<String> returns = new ArrayList<String>(); 
    returns.add(contentPart);
    returns.add(LDID);
    return returns;
  }
  
  public List<String> generateFormContent(String conceptURI, List<String> uriCollection) {
    String contentPart1 = 
    "{"+
      "\"$Components\": ["+
        "{"+
          "\"$Name\": \"LinkedDataForm$formID$\","+
          "\"$Type\": \"LinkedDataForm\","+
          "\"$Version\": \"3\","+
          "\"Uuid\": \"$formUUID$\","+
          "\"FormID\": \"http:\\/\\/punya.appinventor.mit.edu\\/LDFormGenerator_$formTimestamp$\\/\","+
          "\"ObjectType\":" + "\"" + conceptURI + "\"" + ","+
          "\"Width\": \"-2\",";
    
    String contentPart2 = 
          "\"$Components\": ["+
            "{"+
              "\"$Name\": \"TableArrangement$tableID$\","+
              "\"$Type\": \"TableArrangement\","+
              "\"$Version\": \"1\","+
              "\"Uuid\": \"$tableUUID$\","+
              "\"Rows\": \"$tableRowNum$\","+
              "\"Width\": \"-2\","+
              "\"$Components\": [";
     
    String contentPart3 = 
                "{"+
                  "\"$Name\": \"Label$labelID$\","+
                  "\"$Type\": \"Label\","+
                  "\"$Version\": \"2\","+
                  "\"Uuid\": \"$labelUUID$\","+
                  "\"Text\": \"$labelText$\","+
                  "\"Column\": \"$labelCol$\","+
                  "\"Row\": \"$labelRow$\""+
                "}";

    String contentPart4 = 
                "{"+
                  "\"$Name\": \"TextBox$TextBoxID$\","+
                  "\"$Type\": \"TextBox\","+
                  "\"$Version\": \"6\","+
                  "\"Uuid\": \"$TextBoxUUID$\","+
                  "\"Hint\": \"Hint for TextBox1\","+
                  "\"PropertyURI\": \"$TextBoxURI$\","+
                  "\"Column\": \"$TextBoxCol$\","+
                  "\"Row\": \"$TextBoxRow$\""+
                "}";
      
     String contentPart6 = 
              "]"+
            "}"+ 
          "]"+
        "}"+
      "]"+
    "}";

    String formIDRegex = "$formID$";
    String formUUIDRegex = "$formUUID$";
    String formTimestampRegex = "$formTimestamp$";

    String tableIDRegex = "$tableID$"; 
    String tableUUIDRegex = "$tableUUID$"; 
    String tableRowNumRegex = "$tableRowNum$"; 

    String tempFormID = generateRandomLetterOrNum();
    contentPart1 = contentPart1.replace(formIDRegex, tempFormID);
    contentPart1 = contentPart1.replace(formUUIDRegex, System.currentTimeMillis()+"");
    contentPart1 = contentPart1.replace(formTimestampRegex, System.currentTimeMillis()+"");

    contentPart2 = contentPart2.replace(tableIDRegex, generateRandomLetterOrNum());
    contentPart2 = contentPart2.replace(tableUUIDRegex, System.currentTimeMillis()+"");
    contentPart2 = contentPart2.replace(tableRowNumRegex, uriCollection.size()+"");

    String textBoxUri = conceptURI;
    String formContent = generateLabelTextbox(uriCollection, contentPart3, contentPart4);
    String returnPart1 = contentPart1 + contentPart2 + formContent + contentPart6;
    
    List<String> returns = new ArrayList<String>(); 
    returns.add(returnPart1);
    returns.add(tempFormID);
    return returns;
  }
  
  public String generateLabelTextbox(List<String> uriCollection, String contentPart3, String contentPart4) {
    String labelIDRegex = "$labelID$"; 
    String labelUUIDRegex = "$labelUUID$"; 
    String labelTextRegex = "$labelText$"; 
    String labelColRegex = "$labelCol$"; 
    String labelRowRegex = "$labelRow$"; 
    
    String textBoxIDRegex = "$TextBoxID$"; 
    String textBoxUUIDRegex = "$TextBoxUUID$"; 
    String textBoxURIRegex = "$TextBoxURI$"; 
    String textBoxColRegex = "$TextBoxCol$"; 
    String textBoxRowRegex = "$TextBoxRow$"; 
    
    String labelText = "";
    String textBoxUri = "";
    
    String formContent = "";
    String formContentPart1 = "";
    String formContentPart2 = "";
    
   for (int i = 0; i < uriCollection.size(); i++) {
  	 textBoxUri = uriCollection.get(i);
  	 
  	 if (textBoxUri.contains("#")) {
    	 String[] items = textBoxUri.split("#");
    	 labelText = items[items.length-1];
  	 } else {
    	 String[] items = textBoxUri.split("/");
    	 labelText = items[items.length-1];
  	 }

     formContentPart1 = contentPart3.replace(labelIDRegex, generateRandomLetterOrNum());
     formContentPart1 = formContentPart1.replace(labelUUIDRegex, System.currentTimeMillis()+"");
     formContentPart1 = formContentPart1.replace(labelTextRegex, labelText);
     formContentPart1 = formContentPart1.replace(labelColRegex, "0");
     formContentPart1 = formContentPart1.replace(labelRowRegex, i+"");
     
     formContentPart2 = contentPart4.replace(textBoxIDRegex, generateRandomLetterOrNum());
     formContentPart2 = formContentPart2.replace(textBoxUUIDRegex, System.currentTimeMillis()+"");
     formContentPart2 = formContentPart2.replace(textBoxURIRegex, textBoxUri);
     formContentPart2 = formContentPart2.replace(textBoxColRegex, "1");
     formContentPart2 = formContentPart2.replace(textBoxRowRegex, i+"");

     formContent = formContent + formContentPart1 + "," + formContentPart2 + "," ;    	 
   }
   
   if (formContent.endsWith(",") && formContent.length() > 2) {
  	 formContent = formContent.substring(0, formContent.length()-1);
   }
  	return formContent;
  }
  
  public String[] generateBlockContent(String linkedDataFieldName, String linkedDataFormFieldName) {
    String LDNameReg = "LinkedData1";
    String LDFormNameReg = "LinkedDataFormECCCEH";
    
    String contentPart = 
    " <block type=\"local_declaration_statement\" id=\"143\" inline=\"false\" x=\"-5\" y=\"-320\">\n"+
    " <mutation>\n"+
    "   <localname name=\"isAddDataFromLDSuccessful\"></localname>\n"+
    " </mutation>\n"+
    " <field name=\"VAR0\">isAddDataFromLDSuccessful</field>\n"+
    " <value name=\"DECL0\">\n"+
    "   <block type=\"component_method\" id=\"60\" inline=\"false\">\n"+
    "     <mutation component_type=\"LinkedData\" method_name=\"AddDataFromLinkedDataForm\" is_generic=\"false\" instance_name=\"LinkedData1\"></mutation>\n"+
    "     <field name=\"COMPONENT_SELECTOR\">LinkedData1</field>\n"+
    "     <value name=\"ARG0\">\n"+
    "       <block type=\"component_component_block\" id=\"98\">\n"+
    "         <mutation component_type=\"LinkedDataForm\" instance_name=\"LinkedDataFormECCCEH\"></mutation>\n"+
    "         <field name=\"COMPONENT_SELECTOR\">LinkedDataFormECCCEH</field>\n"+
    "       </block>\n"+
    "     </value>\n"+
    "   </block>\n"+
    " </value>\n"+
    " <statement name=\"STACK\">\n"+
    "   <block type=\"component_method\" id=\"30\" inline=\"false\">\n"+
    "     <mutation component_type=\"LinkedData\" method_name=\"AddDataToWeb\" is_generic=\"false\" instance_name=\"LinkedData1\"></mutation>\n"+
    "     <field name=\"COMPONENT_SELECTOR\">LinkedData1</field>\n"+
    "     <value name=\"ARG0\">\n"+
    "       <block type=\"text\" id=\"159\">\n"+
    "         <field name=\"TEXT\"></field>\n"+
    "       </block>\n"+
    "     </value>\n"+
    "     <value name=\"ARG1\">\n"+
    "       <block type=\"logic_boolean\" id=\"166\">\n"+
    "         <field name=\"BOOL\">TRUE</field>\n"+
    "       </block>\n"+
    "     </value>\n"+
    "    </block>\n"+
    "  </statement>\n"+
    " </block>";
    
    contentPart = contentPart.replace(LDNameReg, linkedDataFieldName);
    contentPart = contentPart.replace(LDFormNameReg, linkedDataFormFieldName);
    LOG.info("The blk LD content is \n" + contentPart);
    
    String[] lines = contentPart.split("\\r?\\n");
    return lines;
  }
  
  // 6 random letters &/ numbers
  public String generateRandomLetterOrNum() {
    String val = "";
    // char or numbers (5), random 0-9 A-Z
    for(int i = 0; i<6;){
      int ranAny = 48 + (new Random()).nextInt(90-65);
      if(!(57 < ranAny && ranAny<= 65)){
        char c = (char)ranAny;      
        val += c;
        i++;
      }
    }
    return val;
  }

  @Override
  public long deleteFile(String userId, long projectId, String fileId) {
    if (fileId.endsWith(FORM_PROPERTIES_EXTENSION) ||
        fileId.endsWith(BLOCKLY_SOURCE_EXTENSION)) {
      // If the file to be deleted is a form file or a blocks file, delete both the form file
      // and the blocks file. Also, if there was a codeblocks file laying around
      // for that same form, delete it too (if it doesn't exist the delete
      // for it will be a no-op).
      String qualifiedFormName = YoungAndroidSourceNode.getQualifiedName(fileId);
      String formFileName = YoungAndroidFormNode.getFormFileId(qualifiedFormName);
      String blocklyFileName = YoungAndroidBlocksNode.getBlocklyFileId(qualifiedFormName);
      String codeblocksFileName = YoungAndroidBlocksNode.getCodeblocksFileId(qualifiedFormName);
      String yailFileName = YoungAndroidYailNode.getYailFileId(qualifiedFormName);
      storageIo.deleteFile(userId, projectId, formFileName);
      storageIo.deleteFile(userId, projectId, blocklyFileName);
      storageIo.deleteFile(userId, projectId, codeblocksFileName);
      storageIo.deleteFile(userId, projectId, yailFileName);
      storageIo.removeSourceFilesFromProject(userId, projectId, true,
          formFileName, blocklyFileName, codeblocksFileName, yailFileName);
      return storageIo.getProjectDateModified(userId, projectId);

    } else {
      return super.deleteFile(userId, projectId, fileId);
    }
  }

  /**
   * Make a request to the Build Server to build a project.  The Build Server will asynchronously
   * post the results of the build via the {@link com.google.appinventor.server.ReceiveBuildServlet}
   * A later call will need to be made by the client in order to get those results.
   *
   * @param user the User that owns the {@code projectId}.
   * @param projectId  project id to be built
   * @param nonce random string used to find resulting APK from unauth context
   * @param target  build target (optional, implementation dependent)
   *
   * @return an RpcResult reflecting the call to the Build Server
   */
  @Override
  public RpcResult build(User user, long projectId, String nonce, String target) {
    String userId = user.getUserId();
    String projectName = storageIo.getProjectName(userId, projectId);
    String outputFileDir = BUILD_FOLDER + '/' + target;

    // Store the userId and projectId based on the nonce

    storageIo.storeNonce(nonce, userId, projectId);

    // Delete the existing build output files, if any, so that future attempts to get it won't get
    // old versions.
    List<String> buildOutputFiles = storageIo.getProjectOutputFiles(userId, projectId);
    for (String buildOutputFile : buildOutputFiles) {
      storageIo.deleteFile(userId, projectId, buildOutputFile);
    }
    URL buildServerUrl = null;
    ProjectSourceZip zipFile = null;
    try {
      buildServerUrl = new URL(getBuildServerUrlStr(
          user.getUserEmail(),
          userId,
          projectId,
          outputFileDir));
      HttpURLConnection connection = (HttpURLConnection) buildServerUrl.openConnection();
      connection.setDoOutput(true);
      connection.setRequestMethod("POST");

      BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(connection.getOutputStream());
      FileExporter fileExporter = new FileExporterImpl();
      zipFile = fileExporter.exportProjectSourceZip(userId, projectId, false,
          /* includeAndroidKeystore */ true,
          projectName + ".aia", true, true);
      bufferedOutputStream.write(zipFile.getContent());
      bufferedOutputStream.flush();
      bufferedOutputStream.close();

      int responseCode = 0;
      try {
          responseCode = connection.getResponseCode();
      } catch (IOException e) {
          throw new CouldNotFetchException();
      }
      if (responseCode != HttpURLConnection.HTTP_OK) {
        // Put the HTTP response code into the RpcResult so the client code in BuildCommand.java
        // can provide an appropriate error message to the user.
        // NOTE(lizlooney) - There is some weird bug/problem with HttpURLConnection. When the
        // responseCode is 503, connection.getResponseMessage() returns "OK", but it should return
        // "Service Unavailable". If I make the request with curl and look at the headers, they
        // have the expected error message.
        // For now, the moral of the story is: don't use connection.getResponseMessage().
        String error = "Build server responded with response code " + responseCode + ".";
        try {
          String content = readContent(connection.getInputStream());
          if (content != null && !content.isEmpty()) {
            error += "\n" + content;
          }
        } catch (IOException e) {
          // No content. That's ok.
        }
        try {
          String errorContent = readContent(connection.getErrorStream());
          if (errorContent != null && !errorContent.isEmpty()) {
            error += "\n" + errorContent;
          }
        } catch (IOException e) {
          // No error content. That's ok.
        }
        if (responseCode == HttpURLConnection.HTTP_CONFLICT) {
          // The build server is not compatible with this App Inventor instance. Log this as severe
          // so the owner of the app engine instance will know about it.
          LOG.severe(error);
        }

        return new RpcResult(responseCode, "", StringUtils.escape(error));
      }
    } catch (MalformedURLException e) {
      CrashReport.createAndLogError(LOG, null,
          buildErrorMsg("MalformedURLException", buildServerUrl, userId, projectId), e);
      return new RpcResult(false, "", e.getMessage());
    } catch (IOException e) {
      CrashReport.createAndLogError(LOG, null,
          buildErrorMsg("IOException", buildServerUrl, userId, projectId), e);
      return new RpcResult(false, "", e.getMessage());
    } catch (CouldNotFetchException e) {
        CrashReport.createAndLogError(LOG, null,
                buildErrorMsg("CouldNotFetchException", buildServerUrl, userId, projectId), e);
      return new RpcResult(false, "", " Can not contact the BuildServer at " + buildServerUrl.getHost());
    } catch (EncryptionException e) {
      CrashReport.createAndLogError(LOG, null,
          buildErrorMsg("EncryptionException", buildServerUrl, userId, projectId), e);
      return new RpcResult(false, "", e.getMessage());
    } catch (RuntimeException e) {
      // In particular, we often see RequestTooLargeException (if the zip is too
      // big) and ApiProxyException. There may be others.
      Throwable wrappedException = e;
      if (e instanceof ApiProxy.RequestTooLargeException && zipFile != null) {
        int zipFileLength = zipFile.getContent().length;
        if (zipFileLength >= (5 * 1024 * 1024) /* 5 MB */) {
          wrappedException = new IllegalArgumentException(
              "Sorry, can't package projects larger than 5MB."
              + " Yours is " + zipFileLength + " bytes.", e);
        } else {
          wrappedException = new IllegalArgumentException(
              "Sorry, project was too large to package (" + zipFileLength + " bytes)");
        }
      }
      CrashReport.createAndLogError(LOG, null,
          buildErrorMsg("RuntimeException", buildServerUrl, userId, projectId), wrappedException);
      return new RpcResult(false, "", wrappedException.getMessage());
    }
    return new RpcResult(true, "Building " + projectName, "");
  }

  private String buildErrorMsg(String exceptionName, URL buildURL, String userId, long projectId) {
    return "Request to build failed with " + exceptionName + ", user=" + userId
        + ", project=" + projectId + ", build URL is " + buildURL
        + " [" + buildURL.toString().length() + "]";
  }

  // Note that this is a function rather than just a constant because we assume it will get
  // a little more complicated when we want to get the URL from an App Engine config file or
  // command line argument.
  private String getBuildServerUrlStr(String userName, String userId,
                                      long projectId, String fileName)
      throws UnsupportedEncodingException, EncryptionException {
    return "http://" + buildServerHost.get() + "/buildserver/build-all-from-zip-async"
           + "?uname=" + URLEncoder.encode(userName, "UTF-8")
           + (sendGitVersion.get()
               ? "&gitBuildVersion="
                 + URLEncoder.encode(GitBuildId.getVersion(), "UTF-8")
               : "")
           + "&callback="
           + URLEncoder.encode("http://" + getCurrentHost() + ServerLayout.ODE_BASEURL_NOAUTH
                               + ServerLayout.RECEIVE_BUILD_SERVLET + "/"
                               + Security.encryptUserAndProjectId(userId, projectId)
                               + "/" + fileName,
                               "UTF-8");
  }

  private String getCurrentHost() {
    if (Server.isProductionServer()) {
      if (appengineHost.get()=="") {
        String applicationVersionId = SystemProperty.applicationVersion.get();
        String applicationId = SystemProperty.applicationId.get();
        return applicationVersionId + "." + applicationId + ".appspot.com";
      } else {
        return appengineHost.get();
      }
    } else {
      // TODO(user): Figure out how to make this more generic
      return "localhost:8888";
    }
  }

  /*
   * Reads the UTF-8 content from the given input stream.
   */
  private static String readContent(InputStream stream) throws IOException {
    if (stream != null) {
      BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
      try {
        return CharStreams.toString(reader);
      } finally {
        reader.close();
      }
    }
    return null;
  }

  /**
   * Check if there are any build results available for the given user's project
   *
   * @param user the User that owns the {@code projectId}.
   * @param projectId  project id to be built
   * @param target  build target (optional, implementation dependent)
   * @return an RpcResult reflecting the call to the Build Server. The following values may be in
   *         RpcResult.result:
   *            0:  Build is done and was successful
   *            1:  Build is done and was unsuccessful
   *            2:  Yail generation failed
   *           -1:  Build is not yet done.
   */
  @Override
  public RpcResult getBuildResult(User user, long projectId, String target) {
    String userId = user.getUserId();
    String buildOutputFileName = BUILD_FOLDER + '/' + target + '/' + "build.out";
    List<String> outputFiles = storageIo.getProjectOutputFiles(userId, projectId);
    updateCurrentProgress(user, projectId, target);
    RpcResult buildResult = new RpcResult(-1, ""+currentProgress, ""); // Build not finished
    for (String outputFile : outputFiles) {
      if (buildOutputFileName.equals(outputFile)) {
        String outputStr = storageIo.downloadFile(userId, projectId, outputFile, "UTF-8");
        try {
          JSONObject buildResultJsonObj = new JSONObject(outputStr);
          buildResult = new RpcResult(buildResultJsonObj.getInt("result"),
                                      buildResultJsonObj.getString("output"),
                                      buildResultJsonObj.getString("error"),
                                      outputStr);
        } catch (JSONException e) {
          buildResult = new RpcResult(1, "", "");
        }
        break;
      }
    }
    return buildResult;
  }

  /**
   * Check if there are any build progress available for the given user's project
   *
   * @param user the User that owns the {@code projectId}.
   * @param projectId  project id to be built
   * @param target  build target (optional, implementation dependent)
   */
  public void updateCurrentProgress(User user, long projectId, String target) {
    try {
      String userId = user.getUserId();
      String projectName = storageIo.getProjectName(userId, projectId);
      String outputFileDir = BUILD_FOLDER + '/' + target;
      URL buildServerUrl = null;
      ProjectSourceZip zipFile = null;

      buildServerUrl = new URL(getBuildServerUrlStr(user.getUserEmail(),
        userId, projectId, outputFileDir));
      HttpURLConnection connection = (HttpURLConnection) buildServerUrl.openConnection();
      connection.setDoOutput(true);
      connection.setRequestMethod("POST");

      int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
          try {
            String content = readContent(connection.getInputStream());
            if (content != null && !content.isEmpty()) {
              LOG.info("The current progress is " + content + "%.");
              currentProgress = Integer.parseInt(content);
            }
          } catch (IOException e) {
            // No content. That's ok.
          }
         }
      } catch (MalformedURLException e) {
        // that's ok, nothing to do
      } catch (IOException e) {
        // that's ok, nothing to do
      } catch (EncryptionException e) {
        // that's ok, nothing to do
      } catch (RuntimeException e) {
        // that's ok, nothing to do
      }
  }

  /**
   * Special Exception for the open connect
   */
  class CouldNotFetchException extends Exception {
      String mistake;
      public CouldNotFetchException() {
          super();
          mistake = "Could not fetch the Build Server URL";
      }
  }
}
