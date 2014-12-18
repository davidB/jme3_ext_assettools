* doc: how to use gradle to create a custom asset pipeline (from source/editor file -> conversion -> optimisation -> viewer -> packaging -> publishing)
* doc: how to use gradle to run a command on several view
* doc: how to interactive editing blender/jme by watching file
  * https://github.com/bluepapa32/gradle-watch-plugin
* doc: how to share (local, net) build snippet
  * http://www.gradle.org/docs/current/userguide/userguide_single.html#sec%3aconfiguring_using_external_script
  * http://forums.gradle.org/gradle/topics/apply_buildscript_from_a_common_gradle_file
  * http://www.gradle.org/docs/current/userguide/custom_tasks.html
* doc: how to use blender for level editing
  * [using [Alt]+[D] instead of [Shift]+[D] to duplicate an object](http://www.creativebloq.com/13-blender-tips-pros-7113110)
  * use scenevisitor to extract data, transform,...
  * use name (kind.variant.num) to configure (material,...)
  * use custom propery for unique variation
  * use code (hard code vs yaml, json,...) to apply configuration
  * use interactive pipeline for pseudo realtime viewer
* task: a customizable jme viewer : appsetting + closure (for the init)
* tool: a appstate to update scene (node, geometrie, material) from data receive over local network
* tool: a blender plugin to send data via network for a jme viewer (appstate)
* tool: metrics collector
  * image size (fs, memory, sum) group by kind
  * image count group by resolution
  * total size of a dir (eg : assets), like 'du -k | sort -n'
