package tests;

import models.Project;
import org.testng.annotations.Test;
import utils.TestRail;

public class ProjectTest extends Authorization {

    Project newProject = Project.builder()
            .nameOfProject("New Project")
            .announcement("Project for test")
            .showAnnouncement(true)
            .radio("Use a single repository for all cases (recommended)")
            .build();

    Project newProjectUpd = Project.builder()
            .nameOfProject("Updated Project")
            .announcement("UPD Project for test")
            .showAnnouncement(false)
            .radio("Use a single repository for all cases (recommended)")
            .build();

    @TestRail(testCaseID = {1})
    @Test(description = "Create new project")
    public void isNewProjectCreated() {
        projectSteps
                .openProjectsPage()
                .createProject(newProject)
                .openProjectsPage()
                .validateIsProjectExisted(newProject)
                .deleteProjectAPI(newProject);
    }

    @TestRail(testCaseID = {3})
    @Test(description = "Delete project")
    public void isProjectDeleted() {
        projectSteps
                .openProjectsPage()
                .createProject(newProject)
                .openProjectsPage()
                .deleteProject(newProject)
                .openProjectsPage()
                .validateIsProjectNotExisted(newProject);
    }

    @TestRail(testCaseID = {2})
    @Test(description = "Update project")
    public void isProjectUpdated() {
        projectSteps
                .openProjectsPage()
                .createProject(newProject)
                .openProjectsPage()
                .updateProject(newProject, newProjectUpd)
                .openProjectsPage()
                .openProject(newProjectUpd)
                .validateIsProjectUpdated(newProjectUpd)
                .openProjectsPage()
                .deleteProject(newProjectUpd);
    }
}
