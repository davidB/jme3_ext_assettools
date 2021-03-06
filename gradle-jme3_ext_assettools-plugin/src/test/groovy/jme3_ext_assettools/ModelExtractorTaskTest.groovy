package jme3_ext_assettools;

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test
import static org.junit.Assert.*

class ModelExtractorTaskTest {
	@Test
	public void canAddTaskToProject() {
		Project project = ProjectBuilder.builder().build()
		def task = project.task('greeting', type: ModelExtractorTask)
		assertTrue(task instanceof ModelExtractorTask)
	}

	@Test
	public void extractTeapot() {
		Project project = ProjectBuilder.builder().build()
		ModelExtractorTask task = project.task('extract', type: ModelExtractorTask)
		task.rpath = "Models/Teapot/Teapot.obj"
		task.assetClassLoader = Thread.currentThread().getContextClassLoader()
		task.outBaseName = "teapot"
		task.outDir = "build/test-extract"
		task.execute()
		println(">> " + task.outputs.files.files)
		task.getOutDir().deleteDir()
		//Thread.sleep(10000);
	}

	@Test
	public void extractNinja() {
		Project project = ProjectBuilder.builder().build()
		ModelExtractorTask task = project.task('extract', type: ModelExtractorTask)
		task.rpath = "Models/Ninja/Ninja.mesh.xml"
		task.assetClassLoader = Thread.currentThread().getContextClassLoader()
		//task.outBaseName = "ninja"
		//task.outDir = project.file("build/test-extract")
		//task.prefixTexture = true
		task.execute()
		println(">> " + task.outputs.files.files)
		task.getOutDir().deleteDir()
		//Thread.sleep(10000);
	}

	@Test
	public void extractSponzaObj() {
		Project project = ProjectBuilder.builder().build()
		ModelExtractorTask task = project.task('extract', type: ModelExtractorTask)
		task.file = new File(System.getProperty("user.home"), "Téléchargements/t/crytek/sponza.obj")
		//task.assetClassLoader = Thread.currentThread().getContextClassLoader()
		//task.outBaseName = "ninja"
		//task.outDir = project.file("build/test-extract")
		//task.prefixTexture = true
		task.execute()
		println(">> " + task.outputs.files.files)
		assertEquals(task.outFiles.files, task.outputs.files.files)
		task.getOutDir().deleteDir()
		//Thread.sleep(10000);
	}

}