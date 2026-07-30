# Elastic Web Launcher (Windows x64)

The web bundle is Elastic Dashboard `v2026.1.2`, sourced from the official
[`Elastic-Web.zip`](https://github.com/Gold872/elastic_dashboard/releases/download/v2026.1.2/Elastic-Web.zip)
release asset.

Archive SHA-256:
`6865537c1e2281b73a54aba475e9fa0d30fd270cc2f2cab62b7949e3578e3f47`

After normalizing line endings, the upstream bundle files match the release archive.
The upstream `start_elastic.py` is omitted; Team 8214 adds `layout/` and the Windows
launcher under `launcher/`. Elastic is MIT-licensed and its bundled third-party notices
remain in `assets/NOTICES` and `assets/assets/third_party_licenses/`.

The launcher serves the built web files in this folder using a small Go HTTP server.

## What this launcher does

The launcher:
- Reads the executable file name as the port number.
- Serves the current folder over HTTP on `127.0.0.1:<port>`.

Example:
- `5900.exe` -> serves on `http://127.0.0.1:5900`
- `5901.exe` -> serves on `http://127.0.0.1:5901`

Port must be in range `1024-65535`.

## 1) Install Go on Windows x64

1. Open the official download page: https://go.dev/dl/
2. Download the Windows installer for AMD64 (`windows-amd64.msi`).
3. Run the installer and keep default options.
4. Open a new PowerShell window and verify:

```powershell
go version
```

You should see something like `go version go1.xx.x windows/amd64`.

## 2) Build the launcher

Open PowerShell in the `launcher` folder, then run:

```powershell
go build -o 5900.exe .\elastic_launcher.go
```

This creates `5900.exe`.

To build another port-specific executable, change the output name:

```powershell
go build -o 5901.exe .\elastic_launcher.go
```

## 3) Run and use

1. Make sure you are in the `.elastic` folder (the folder that contains `index.html`).
2. Start the launcher:

```powershell
.\launcher\5900.exe
```

3. Open in browser:

```text
http://127.0.0.1:5900
```

Press `Ctrl+C` in the terminal to stop the server.

## Troubleshooting

- `UI theme color`:
    - 9A29FF
- `go is not recognized`:
    - Reopen terminal after installation.
    - Confirm Go is installed and in `PATH`.
- `Invalid port number in filename`:
    - Rename executable to a numeric name, e.g. `5900.exe`.
- `Port ... is out of valid range`:
    - Use a filename between `1024` and `65535`.
- Browser cannot connect:
    - Check whether another process already uses that port.
    - Confirm you started the `.exe` from this project directory.
