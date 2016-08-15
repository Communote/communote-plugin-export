# About
The Communote export plugin is a plugin for [Communote](https://github.com/Communote/communote-server) which adds the ability to export the data of a Communote installation in an XML format. The export covers users, groups, group memberships, topics with permissions, information about which items a user follows and of course the notes. The notes are extracted with tags, mentions, likes, comments and attachments. Additionally the bookmarked notes of users are also exported.

# Compatibility
The plugin can be used with a Communote standalone installation and the Communote SaaS platform.

The following table shows which Communote server versions are supported by a specific version of the plugin. A server version which is not listed cannot be uses with the plugin.

| Plugin Version  | Supported Server Version |
| ------------- | ------------- |
| 1.2  | 3.4  |

# Installation
To install the plugin get a release from the [Releases](https://github.com/Communote/communote-plugin-export/releases) section and deploy to yout Communote installation as described in the [Installation Documentation](http://communote.github.io/doc/install_extensions.html).

# Usage
After installing the plugin a new page named 'Export data' will be available in the administration section of Communote under 'Extensions'. There the export can be scheduled with a click on 'Start export'. Depending on the amount of data the export can take a while. As soon as it is finished a ZIP file can be downloaded. This file contains

* a directory with all attachments
* the file users.xml with data about the users
* the file topics.xml with details about the topics
* the file notes.xml with data about the notes including meta-data (likes, mentions, ...) and references to the attachments
* the file groups.xml with information about the user groups
* the file groupMembers.xml with details about the group memberships
* the file follows.xml with data about the items which are followed by users
* an XSD describing the XML files
 
After completing an export a new one can be started with 'Restart export' button to get the updated content.

# Building
To build the plugin you have to clone or download the source and setup your development environment as described in our [Developer Documentation](http://communote.github.io/doc/home.html). Afterwards you can just run ```mvn``` in the checkout directory. The JAR file will be created in the target subdirectory.

# License
The plugin is licensed under the [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0).
