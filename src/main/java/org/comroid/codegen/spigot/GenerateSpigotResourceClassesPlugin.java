package org.comroid.codegen.spigot;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class GenerateSpigotResourceClassesPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        var task = project.getTasks().register("generateSpigotResourceClasses", GenerateSpigotResourceClassesTask.class, it -> {
            it.setGroup("build");
            it.setDescription("Generates Resource Accessors for Spigot plugin.yml and other resources");
        });

        project.getTasks().named("compileJava", compileJavaTask -> compileJavaTask.dependsOn(task));
    }
}