package com.lbrowser.lbrowser.dialogs;

import com.lbrowser.lbrowser.modes.DockerManager;
import io.qt.core.QThread;

public class DockerSetupTask extends QThread {
    public final Signal2<Integer, String> progressUpdated = new Signal2<>();
    public final Signal2<Boolean, String> taskFinished = new Signal2<>();
    private final DockerManager dockerManager;

    public DockerSetupTask(DockerManager manager) {
        if (manager == null){
            throw new NullPointerException("DockerManager cannot be null for DockerSetupTask");
        }
        this.dockerManager = manager;
    }

    @Override
    public void run() {
        try{
            emitProgress(10, "Verifying Docker service...");
            if(!dockerManager.isDockerRunning()){
                emitFinished(false, "Docker service is not running. Please start Docker and try again.");
                return;
            }

            emitProgress(20, "Docker is running...");
            QThread.msleep(200);

            emitProgress(30, "Verifying Portainer service...");
            boolean portainerIsRunning = dockerManager.isPortainerRunning();
            QThread.msleep(200);

            if(!portainerIsRunning) {
                emitProgress(45, "Portainer is not running. Starting Portainer...");
                if(!dockerManager.startPortainerContainer()) {
                    emitFinished(false, "Failed to start Portainer container. Please check Docker settings.");
                    return;
                }
                emitProgress(60, "Portainer started successfully.");
            }else{
                emitProgress(60, "Portainer is already running.");
            }
            QThread.msleep(200);

            emitProgress(70, "Loading Network containers...");
            if(!dockerManager.startApplicationServices()){
                emitFinished(false, "Failed to start Application Services. Please check Docker settings.");
                return;
            }
            emitProgress(85, "Application Services started successfully.");
            QThread.msleep(500);

            emitProgress(100, "Docker setup completed successfully. Opening Portainer...");
            QThread.msleep(500);

            emitFinished(true, "Process completed successfully. Portainer is running.");
        }catch (Exception e){
            e.printStackTrace();
            emitFinished(false, "Error: " + e.getMessage());
        }
    }

    private void emitProgress(int value, String message){
        progressUpdated.emit(value, message);
    }

    private void emitFinished(boolean success, String message){
        taskFinished.emit(success, message);
    }
}
