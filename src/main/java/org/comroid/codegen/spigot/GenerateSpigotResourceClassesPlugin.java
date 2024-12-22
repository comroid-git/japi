package org.comroid.codegen.spigot;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskContainer;

public class GenerateSpigotResourceClassesPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        var tasks = project.getTasks();
        tasks.register("generateSpigotResourceClasses", GenerateSpigotResourceClassesTask.class, it -> {
            it.setGroup("build");
            it.setDescription("Generates Resource Accessors for Spigot plugin.yml and other resources");
            it.doLast(task->{
                var cgj = tasks.getByName("compileGeneratedJava").dependsOn(task);
                tasks.getByName("compileJava").dependsOn(cgj);
            });
        });
    }
}