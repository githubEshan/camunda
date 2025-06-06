package start

import (
	"context"
	"fmt"
	"os"
	"os/exec"
	"path/filepath"
	"runtime"
	"strconv"
	"strings"
	"sync"

	"github.com/camunda/camunda/c8run/internal/health"
	"github.com/camunda/camunda/c8run/internal/overrides"
	"github.com/camunda/camunda/c8run/internal/processmanagement"
	"github.com/camunda/camunda/c8run/internal/types"
	"github.com/rs/zerolog/log"
)

func printSystemInformation(javaVersion, javaHome, javaOpts string) {
	fmt.Println("")
	fmt.Println("")
	fmt.Println("System Version Information")
	fmt.Println("--------------------------")
	fmt.Println("Camunda Details:")
	fmt.Printf("  Version: %s\n", os.Getenv("CAMUNDA_VERSION"))
	fmt.Println("--------------------------")
	fmt.Println("Java Details:")
	fmt.Printf("  Version: %s\n", javaVersion)
	fmt.Printf("  JAVA_HOME: %s\n", javaHome)
	fmt.Printf("  JAVA_OPTS: %s\n", javaOpts)
	fmt.Println("--------------------------")
	fmt.Println("Logging Details:")
	fmt.Println("  Elasticsearch: ./log/elasticsearch.log")
	fmt.Println("  Connectors: ./log/connectors.log")
	fmt.Println("  Camunda: ./log/camunda.log")
	fmt.Println("--------------------------")
	fmt.Println("Press Ctrl+C to initiate graceful shutdown.")
	fmt.Println("--------------------------")
	fmt.Println("")
	fmt.Println("")
}

func getJavaVersion(javaBinary string) (string, error) {
	javaVersionCmd := exec.Command(javaBinary, "JavaVersion")
	var out strings.Builder
	var stderr strings.Builder
	javaVersionCmd.Stdout = &out
	javaVersionCmd.Stderr = &stderr
	err := javaVersionCmd.Run()
	if err != nil {
		return "", fmt.Errorf("failed to run java version command: %w", err)
	}
	javaVersionOutput := out.String()
	return javaVersionOutput, nil
}

type StartupHandler struct {
	ProcessHandler *processmanagement.ProcessHandler
}

func (s *StartupHandler) startApplication(cmd *exec.Cmd, pid string, logPath string, stop context.CancelFunc) error {
	logFile, err := os.OpenFile(logPath, os.O_RDWR|os.O_CREATE, 0644)
	if err != nil {
		log.Err(err).Msg("Failed to open file: " + logPath)
		return err
	}
	cmd.Stdout = logFile
	cmd.Stderr = logFile
	err = cmd.Start()
	if err != nil {
		log.Err(err).Msg("Failed to start process: " + cmd.String())
		return err
	}

	err = s.ProcessHandler.WritePIDToFile(pid, cmd.Process.Pid)
	if err != nil {
		log.Err(err).Msg("Failed to write PID to file: " + pid)
		log.Info().Msg("To avoid zombie processes, we will now kill all processes that have the same PID as the process we just started and quit the application")
		stop()
		return err
	}

	return nil
}

func getJavaHome(javaBinary string) (string, error) {
	javaHomeCmd := exec.Command(javaBinary, "JavaHome")
	var out strings.Builder
	var stderr strings.Builder
	javaHomeCmd.Stdout = &out
	javaHomeCmd.Stderr = &stderr
	err := javaHomeCmd.Run()
	if err != nil {
		return "", fmt.Errorf("failed to run java version command: %w", err)
	}
	javaHomeOutput := out.String()
	return javaHomeOutput, nil
}

func resolveJavaHomeAndBinary() (string, string, error) {
	javaHome := os.Getenv("JAVA_HOME")
	javaBinary := "java"
	var javaHomeAfterSymlink string
	var err error
	if javaHome != "" {
		javaHomeAfterSymlink, err = filepath.EvalSymlinks(javaHome)
		if err != nil {
			log.Debug().Err(err).Msg("JAVA_HOME is not a valid path, obtaining JAVA_HOME from java binary")
			javaHome = ""
		} else {
			javaHome = javaHomeAfterSymlink
		}
	}
	if javaHome == "" {
		javaHome, err = getJavaHome(javaBinary)
		if err != nil {
			return "", "", fmt.Errorf("failed to get JAVA_HOME")
		}
	}

	if javaHome != "" {
		err = filepath.Walk(javaHome, func(path string, info os.FileInfo, err error) error {
			_, filename := filepath.Split(path)
			if strings.Compare(filename, "java.exe") == 0 || strings.Compare(filename, "java") == 0 {
				javaBinary = path
				return filepath.SkipAll
			}
			return nil
		})
		if err != nil {
			return "", "", err
		}
		// fallback to bin/java.exe
		if javaBinary == "" {
			if runtime.GOOS == "linux" || runtime.GOOS == "darwin" {
				javaBinary = filepath.Join(javaHome, "bin", "java")
			} else if runtime.GOOS == "windows" {
				javaBinary = filepath.Join(javaHome, "bin", "java.exe")
			}
		}
	} else {
		path, err := exec.LookPath("java")
		if err != nil {
			return "", "", fmt.Errorf("failed to find JAVA_HOME or java program")
		}

		// go up 2 directories since it's not guaranteed that java is in a bin folder
		javaHome = filepath.Dir(filepath.Dir(path))
		javaBinary = path
	}

	return javaHome, javaBinary, nil
}

func (s *StartupHandler) StartCommand(wg *sync.WaitGroup, ctx context.Context, stop context.CancelFunc, state *types.State, parentDir string) {
	defer wg.Done()

	c8 := state.C8
	settings := state.Settings
	processInfo := state.ProcessInfo

	// Rresolve JAVA_HOME and javaBinary
	javaHome, javaBinary, err := resolveJavaHomeAndBinary()
	if err != nil {
		fmt.Println(err.Error())
		os.Exit(1)
	}

	err = overrides.SetEnvVars(javaHome)
	if err != nil {
		fmt.Println("Failed to set envVars:", err)
	}
	javaVersion := os.Getenv("JAVA_VERSION")
	if javaVersion == "" {
		javaVersion, err = getJavaVersion(javaBinary)
		if err != nil {
			fmt.Println("Failed to get Java version")
			os.Exit(1)
		}
	}

	expectedJavaVersion := 21

	versionSplit := strings.Split(javaVersion, ".")
	if len(versionSplit) == 0 {
		fmt.Println("Java needs to be installed. Please install JDK " + strconv.Itoa(expectedJavaVersion) + " or newer.")
		os.Exit(1)
	}
	javaMajorVersion := versionSplit[0]
	javaMajorVersionInt, _ := strconv.Atoi(javaMajorVersion)
	if javaMajorVersionInt < expectedJavaVersion {
		fmt.Print("You must use at least JDK " + strconv.Itoa(expectedJavaVersion) + " to start Camunda Platform Run.\n")
		os.Exit(1)
	}

	javaOpts := os.Getenv("JAVA_OPTS")
	if javaOpts != "" {
		fmt.Print("JAVA_OPTS: " + javaOpts + "\n")
	}
	javaOpts = overrides.AdjustJavaOpts(javaOpts, settings)

	printSystemInformation(javaVersion, javaHome, javaOpts)
	elasticHealthEndpoint := "http://localhost:9200/_cluster/health?wait_for_status=green&wait_for_active_shards=all&wait_for_no_initializing_shards=true&timeout=120s"
	if !settings.DisableElasticsearch {
		s.ProcessHandler.AttemptToStartProcess(processInfo.Elasticsearch.PidPath, "Elasticsearch", func() {
			elasticsearchCmd := c8.ElasticsearchCmd(ctx, processInfo.Elasticsearch.Version, parentDir)
			elasticsearchLogFilePath := filepath.Join(parentDir, "log", "elasticsearch.log")
			err := s.startApplication(elasticsearchCmd, processInfo.Elasticsearch.PidPath, elasticsearchLogFilePath, stop)
			if err != nil {
				log.Err(err).Msg("Failed to start Elasticsearch")
				stop()
				return
			}
		}, func() error {
			return health.QueryElasticsearch(ctx, "Elasticsearch", 12, elasticHealthEndpoint)
		}, stop)
	}

	var extraArgs string
	if settings.Config != "" {
		path := filepath.Join(parentDir, settings.Config)
		var slash string
		switch runtime.GOOS {
		case "linux", "darwin":
			slash = "/"
		case "windows":
			slash = "\\"
		}

		configStat, err := os.Stat(path)
		if err != nil {
			fmt.Printf("Failed to read config file: %s\n", path)
			os.Exit(1)
		}
		if configStat.IsDir() {
			extraArgs = "--spring.config.additional-location=file:" + filepath.Join(parentDir, settings.Config) + slash
		} else {
			extraArgs = "--spring.config.additional-location=file:" + filepath.Join(parentDir, settings.Config)
		}
	}

	s.ProcessHandler.AttemptToStartProcess(processInfo.Connectors.PidPath, "Connectors", func() {
		connectorsCmd := c8.ConnectorsCmd(ctx, javaBinary, parentDir, processInfo.Camunda.Version)
		connectorsLogPath := filepath.Join(parentDir, "log", "connectors.log")
		err := s.startApplication(connectorsCmd, processInfo.Connectors.PidPath, connectorsLogPath, stop)
		if err != nil {
			log.Err(err).Msg("Failed to start Connectors process")
			stop()
			return
		}
	}, func() error {
		// TODO do a health check on the connectors process
		return nil
	}, stop)

	s.ProcessHandler.AttemptToStartProcess(processInfo.Camunda.PidPath, "Camunda", func() {
		camundaCmd := c8.CamundaCmd(ctx, processInfo.Camunda.Version, parentDir, extraArgs, javaOpts)
		camundaLogPath := filepath.Join(parentDir, "log", "camunda.log")
		err := s.startApplication(camundaCmd, processInfo.Camunda.PidPath, camundaLogPath, stop)
		if err != nil {
			log.Err(err).Msg("Failed to start Camunda process")
			stop()
			return
		}
	}, func() error {
		return health.QueryCamunda(ctx, c8, "Camunda", settings, 24)
	}, stop)
}
