package jme3_ext_assettools;

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test
import static org.junit.Assert.*

class ExtractModelTaskTest {
	@Test
	public void canAddTaskToProject() {
		Project project = ProjectBuilder.builder().build()
		def task = project.task('greeting', type: ExtractModelTask)
		assertTrue(task instanceof ExtractModelTask)
	}

	//@Test
	public void extractTeapot() {
		Project project = ProjectBuilder.builder().build()
		ExtractModelTask task = project.task('extract', type: ExtractModelTask)
		task.inRPath = "Models/Teapot/Teapot.obj"
		task.assetClassLoader = Thread.currentThread().getContextClassLoader()
		task.outBaseName = "teapot"
		task.outDir = project.file("build/test-extract")
		task.execute()
		println(task.files)
		task.outDir.deleteDir()
		//Thread.sleep(10000);
	}

	//@Test
	public void extractNinja() {
		Project project = ProjectBuilder.builder().build()
		ExtractModelTask task = project.task('extract', type: ExtractModelTask)
		task.inRPath = "Models/Ninja/Ninja.mesh.xml"
		task.assetClassLoader = Thread.currentThread().getContextClassLoader()
		//task.outBaseName = "ninja"
		//task.outDir = project.file("build/test-extract")
		//task.prefixTexture = true
		task.execute()
		println(task.files)
		task.outDir.deleteDir()
		//Thread.sleep(10000);
	}

	@Test
	public void extractSponzaObj() {
		Project project = ProjectBuilder.builder().build()
		ExtractModelTask task = project.task('extract', type: ExtractModelTask)
		task.inFile = new File(System.getProperty("user.home"), "Téléchargements/t/crytek/sponza.obj")
		//task.assetClassLoader = Thread.currentThread().getContextClassLoader()
		//task.outBaseName = "ninja"
		//task.outDir = project.file("build/test-extract")
		//task.prefixTexture = true
		task.execute()
		println(task.files)
		task.outDir.deleteDir()
		//Thread.sleep(10000);
	}

}