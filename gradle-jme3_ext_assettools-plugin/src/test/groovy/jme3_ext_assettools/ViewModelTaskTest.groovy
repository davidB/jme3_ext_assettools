package jme3_ext_assettools;

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test
import static org.junit.Assert.*

class ViewModelTaskTest {
	@Test
	public void canAddTaskToProject() {
		Project project = ProjectBuilder.builder().build()
		def task = project.task('greeting', type: ViewModelTask)
		assertTrue(task instanceof ViewModelTask)
	}

	@Test
	public void showModel() {
		Project project = ProjectBuilder.builder().build()
		ViewModelTask task = project.task('view', type: ViewModelTask)
		task.rpath = "Models/Teapot/Teapot.obj"
		task.assetClassLoader = Thread.currentThread().getContextClassLoader()
		task.execute()
		//Thread.sleep(10000);
	}

}