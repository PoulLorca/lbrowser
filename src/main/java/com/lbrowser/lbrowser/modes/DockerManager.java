package com.lbrowser.lbrowser.modes;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DockerManager {
    private final String dockerComposeFilePath;
    private static final List<String> DOCKER_COMMANDS = Arrays.asList("docker", "compose");
    private static final List<String> FALLBACK_DOCKER_COMMANDS = Arrays.asList("docker-compose");
    
    public DockerManager(String dockerComposeFilePath) {
        if(dockerComposeFilePath == null || dockerComposeFilePath.trim().isEmpty()) {
            throw new IllegalArgumentException("Docker compose file path cannot be null or empty");
        }
        
        File composeFile = new File(dockerComposeFilePath);
        if(!composeFile.exists()) {
            System.err.println("Docker compose file does not exist: " + dockerComposeFilePath);
        }
        this.dockerComposeFilePath = dockerComposeFilePath;
    }
    
    private String[] executeCommand(List<String> command, File workingDirectory){
        StringBuilder output = new StringBuilder();
        int exitCode = -1;
        
        Process process = null;
        try{
            ProcessBuilder pb = new ProcessBuilder(command);
            if(workingDirectory != null) {
                pb.directory(workingDirectory);
            }
            pb.redirectErrorStream(true);
            
            process = pb.start();
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))){
                String line;
                while((line = reader.readLine()) != null) {
                    output.append(line).append(System.lineSeparator());
                }
            }

            exitCode = process.waitFor();
            
        }catch (IOException | InterruptedException e) {
            output.append(e.getMessage()).append(System.lineSeparator());
            exitCode = -1;
            e.printStackTrace();
        }finally {
            if(process != null) {
                try{process.getInputStream().close();}catch (IOException e) { }
                try{process.getOutputStream().close();}catch (IOException e) { }
                try{process.getErrorStream().close();}catch (IOException e) { }
            }
        }
        
        return new String[]{String.valueOf(exitCode), output.toString()};
    }
    
    public boolean isDockerRunning(){
        List<String> command = List.of("docker", "info");
        String[] result = executeCommand(command, null);
        
        int exitCode = Integer.parseInt(result[0]);
        String output = result[1];

        if (exitCode == 0) {
            if (output.toLowerCase().contains("cannot connect to the docker daemon") ||
                    output.toLowerCase().contains("is the docker daemon running?") ||
                    output.toLowerCase().contains("error during connect")) {
                System.err.println("Docker info output indicates daemon is not running: " + output.trim());
                return false;
            }
            System.out.println("Docker info check successful.");
            return true;
        } else {
            System.err.println("Docker info command failed with exit code " + exitCode + ". Output:\n" + output.trim());
            return false;
        }
    }

    public boolean isPortainerRunning(){
        List<String> command = List.of("docker", "container", "inspect", "portainer");
        String[] result = executeCommand(command, null);

        int exitCode = Integer.parseInt(result[0]);
        String output = result[1];

        if(exitCode == 0) {
            boolean isRunning = output.contains("\"Status\": \"running\"");
            return isRunning;
        }else if (exitCode != 0) {
            System.err.println("Docker compose command failed with exit code " + exitCode + ". Output:\n" + output.trim());
            return false;
        }
        return false;
    }

    public boolean startPortainerContainer(){
        List<String> command = new ArrayList<>(Arrays.asList(
                "docker", "run", "-d",
                "-p", "8000:8000",
                "-p", "9000:9000",
                "--name", "portainer",
                "--restart", "always",
                "-v", "/var/run/docker.sock:/var/run/docker.sock",
                "-v", "portainer_data:/data",
                "portainer/portainer-ce:latest"
        ));

        System.out.println("Attemping to run Portainer container...");
        String[] result = executeCommand(command, null);
        int exitCode = Integer.parseInt(result[0]);
        String output = result[1];

        if(exitCode == 0) {
            System.out.println("Portainer container started successfully.");
            return true;
        }else{
            System.out.println("Failed to start Portainer container. Exit code: " + exitCode);
            if(output.toLowerCase().contains("bind for 0.0.0.0:9000 failed: port is already allocated") ||
                    output.toLowerCase().contains("bind for 0.0.0.0:8000 failed: port is already allocated")) {
                System.err.println("Portainer container is already running or ports are already in use.");
            }
            return false;
        }
    }

    public boolean startApplicationServices(){
        File composeFile = new File(dockerComposeFilePath);
        if(!composeFile.exists()) {
            System.err.println("Docker compose file does not exist: " + dockerComposeFilePath);
            return false;
        }

        List<String> commandCompose = Arrays.asList("docker", "compose", "-f", dockerComposeFilePath, "create");
        List<String> commandComposeDash = Arrays.asList("docker-compose", "-f", dockerComposeFilePath, "create");

        String[] result = executeCommand(commandCompose, composeFile.getParentFile());
        int exitCode = Integer.parseInt(result[0]);
        String output = result[1];

        if (exitCode != 0 && (output.toLowerCase().contains("command not found") || output.toLowerCase().contains("executable file not found"))) {
            System.out.println("'docker compose' not found for up command, trying 'docker-compose'...");
            result = executeCommand(commandComposeDash, composeFile.getParentFile());
            exitCode = Integer.parseInt(result[0]);
            output = result[1];
        } else if (exitCode != 0) {
            System.err.println("'docker compose up' failed with exit code " + exitCode + ". Output:\n" + output.trim());
        }

        if (exitCode == 0) {
            System.out.println("docker-compose up -d successful for application services. Output:\n" + output.trim());
            return true;
        } else {
            System.err.println("Failed to start application services using docker-compose. Final exit code " + exitCode + ". Output:\n" + output.trim());
            return false;
        }
    }
}
