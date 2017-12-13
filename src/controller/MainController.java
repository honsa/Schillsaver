package controller;

import com.valkryst.VMVC.Application;
import com.valkryst.VMVC.Settings;
import com.valkryst.VMVC.controller.Controller;
import javafx.collections.FXCollections;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.control.ListView;
import misc.Job;
import model.MainModel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import view.MainView;

import java.util.List;

public class MainController extends Controller<MainModel, MainView> implements EventHandler {
    /**
     * Constructs a new MainController.
     *
     * @param application
     *          The driver.
     */
    public MainController(final Application application) {
        super(application, new MainModel(), new MainView());
        addEventHandlers();
    }

    /**
     * Sets all of the view's controls to use this class as their
     * event handler.
     */
    private void addEventHandlers() {
        view.getButton_createJob().setOnAction(this);
        view.getButton_editJob().setOnAction(this);
        view.getButton_deleteSelectedJobs().setOnAction(this);
        view.getButton_processJobs().setOnAction(this);

        view.getButton_programSettings().setOnAction(this);
    }

    @Override
    public void handle(final Event event) {
        final Object source = event.getSource();

        if (source.equals(view.getButton_createJob())) {
            if (view.getButton_createJob().isDisabled() == false) {
                openJobView();
            }
        }

        if (source.equals(view.getButton_editJob())) {
            if (view.getButton_editJob().isDisabled() == false) {
                openEditJobView();
            }
        }

        if (source.equals(view.getButton_deleteSelectedJobs())) {
            if (view.getButton_deleteSelectedJobs().isDisabled() == false) {
                deleteSelectedJobs();
            }
        }

        if (source.equals(view.getButton_processJobs())) {
            if (view.getButton_processJobs().isDisabled() == false) {
                // Disable Buttons:
                view.getButton_createJob().setDisable(true);
                view.getButton_editJob().setDisable(true);
                view.getButton_deleteSelectedJobs().setDisable(true);
                view.getButton_processJobs().setDisable(true);
                view.getButton_programSettings().setDisable(true);

                final Settings settings = getApplication().getSettings();

                final List<Thread> encodeJobs = model.prepareEncodingJobs(settings, view);
                final List<Thread> decodeJobs = model.prepareDecodingJobs(settings, view);

                final Thread thread = new Thread(() -> {
                    processJobs(encodeJobs, decodeJobs);

                    // Enable Buttons:
                    view.getButton_createJob().setDisable(false);
                    view.getButton_editJob().setDisable(false);
                    view.getButton_deleteSelectedJobs().setDisable(false);
                    view.getButton_processJobs().setDisable(false);
                    view.getButton_programSettings().setDisable(false);
                });

                thread.start();
            }
        }

        if (source.equals(view.getButton_programSettings())) {
            if (view.getButton_programSettings().isDisabled() == false) {
                editProgramSettings();
            }
        }
    }

    /** Deserializes the jobs from a file, if the file exists. */
    public void loadJobsFromFile() {
        model.loadJobs();

        for (final Job job : model.getJobs().values()) {
            view.getJobsList().getItems().add(job.getName());
        }
    }

    /** Serializes the jobs to a file. */
    public void saveJobsToFile() {
        model.saveJobs();
    }

    /** Opens the JobView. */
    private void openJobView() {
        final JobController controller = new JobController(getApplication());
        getApplication().swapToNewScene(controller);
    }

    /**
     * Opens the JobView with the first of the currently selected Jobs.
     *
     * If no Jobs are selected, then nothing happens.
     */
    private void openEditJobView() {
        final ListView<String> jobList = view.getJobsList();
        final List<String> selectedJobs = jobList.getSelectionModel().getSelectedItems();

        if (selectedJobs.size() == 0) {
            return;
        }

        final String firstJobName = selectedJobs.get(0);
        final Job job = model.getJobs().get(firstJobName);

        final JobController controller = new JobController(getApplication());
        controller.editJob(job);

        getApplication().swapToNewScene(controller);
    }

    /**
     * Adds a job into the model and the job list.
     *
     * If a job of the same name already exists, then an error is shown
     * and nothing happens.
     *
     * @param job
     *          The job.
     */
    public void addJob(final Job job) {
        final String jobName = job.getName();

        if (! containsJob(jobName)) {
            view.getJobsList().getItems().add(job.getName());
        }

        model.getJobs().put(jobName, job);
        model.saveJobs();
    }

    /**
     * Determines whether the model contains a job with a specific name.
     *
     * @param jobName
     *          The name.
     *
     * @return
     *          Whether the model contains a job with the name.
     */
    public boolean containsJob(final String jobName) {
        return model.getJobs().containsKey(jobName);
    }

    /** Deletes all jobs selected within the view's job list. */
    private void deleteSelectedJobs() {
        final ListView<String> jobsList = view.getJobsList();
        final List<String> selectedJobs = FXCollections.observableArrayList(jobsList.getSelectionModel().getSelectedItems());

        for (final String jobName : selectedJobs) {
            view.getJobsList().getItems().remove(jobName);
            model.getJobs().remove(jobName);
        }

        jobsList.getSelectionModel().clearSelection();
        model.saveJobs();
    }

    private void processJobs(final List<Thread> encodeJobs, final List<Thread> decodeJobs) {
        // Run Encode Jobs
        final Thread mainEncodingThread = new Thread(() -> {
           for (final Thread thread : encodeJobs) {
               thread.start();

               try {
                   thread.join();
               } catch (final InterruptedException e) {
                   // todo Look into exception cause, maybe tell user
                   final Logger logger = LogManager.getLogger();
                   logger.error(e);

                   e.printStackTrace();
               }
           }
        });

        mainEncodingThread.start();

        // Run Decode Jobs
        final Thread mainDecodingThread = new Thread(() -> {
            for (final Thread thread : decodeJobs) {
                thread.start();

                try {
                    thread.join();
                } catch (final InterruptedException e) {
                    // todo Look into exception cause, maybe tell user
                    final Logger logger = LogManager.getLogger();
                    logger.error(e);

                    e.printStackTrace();
                }
            }
        });

        mainDecodingThread.start();

        try {
            mainEncodingThread.join();
            mainDecodingThread.join();
        } catch (InterruptedException e) {
            // todo Look into exception cause, maybe tell user
            final Logger logger = LogManager.getLogger();
            logger.error(e);

            e.printStackTrace();
        }
    }

    private void editProgramSettings() {

    }
}
