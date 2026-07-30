package main

import (
	"fmt"
	"log"
	"net/http"
	"os"
	"path/filepath"
	"strconv"
	"strings"
)

func main() {
	// Get the executable file name without the extension.
	exePath, err := os.Executable()
	if err != nil {
		log.Fatal("Failed to get executable path:", err)
	}

	exeName := filepath.Base(exePath)
	// Remove the .exe extension.
	exeName = strings.TrimSuffix(exeName, filepath.Ext(exeName))

	// Parse the file name as the port number.
	port, err := strconv.Atoi(exeName)
	if err != nil {
		log.Fatalf("Invalid port number in filename '%s'. Please rename the exe to a valid port number (e.g., 5800.exe)", exeName)
	}

	// Validate the port range.
	if port < 1024 || port > 65535 {
		log.Fatalf("Port %d is out of valid range (1024-65535)", port)
	}

	// Create the file server.
	exeDir := filepath.Dir(exePath)
	parentDir := filepath.Dir(exeDir)
	fs := http.FileServer(http.Dir(parentDir))
	addr := fmt.Sprintf("127.0.0.1:%d", port)

	fmt.Printf("🚀 Elastic server starting...\n")
	fmt.Printf("   Port: %d\n", port)
	fmt.Printf("   URL:  http://%s\n", addr)
	fmt.Printf("\nPress Ctrl+C to stop the server\n\n")

	// Start the server.
	if err := http.ListenAndServe(addr, fs); err != nil {
		log.Fatal("Server error:", err)
	}
}
