# PowerShell script to run the application with proper JInput setup
Write-Host "Setting up JInput native libraries..." -ForegroundColor Green

# Clean and build
Write-Host "Building application..." -ForegroundColor Yellow
mvn clean package -DskipTests

# Create natives directory
if (!(Test-Path "natives")) {
    New-Item -ItemType Directory -Path "natives"
}

# Extract native libraries
Write-Host "Extracting native libraries..." -ForegroundColor Yellow
$jarFile = "target/motor-control-1.0-SNAPSHOT-jar-with-dependencies.jar"

if (Test-Path $jarFile) {
    # Use PowerShell to extract specific DLLs
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $jar = [IO.Compression.ZipFile]::OpenRead($jarFile)
    
    foreach ($entry in $jar.Entries) {
        if ($entry.Name -match "jinput.*\.dll$") {
            Write-Host "Extracting: $($entry.Name)" -ForegroundColor Cyan
            $dest = Join-Path "natives" $entry.Name
            [IO.Compression.ZipFileExtensions]::ExtractToFile($entry, $dest, $true)
        }
    }
    $jar.Dispose()
    
    # List extracted files
    Write-Host "Extracted native libraries:" -ForegroundColor Green
    Get-ChildItem "natives" -Filter "*.dll" | ForEach-Object { Write-Host "  - $($_.Name)" }
    
    # Run the application
    Write-Host "Running application with gamepad support..." -ForegroundColor Green
    Write-Host "Controller Priority: Gamepad (40) > Keyboard (30)" -ForegroundColor Yellow
    Write-Host "Gamepad will take control when in use, keyboard when gamepad is idle" -ForegroundColor Yellow
    java "-Djava.library.path=natives" -jar $jarFile
    
} else {
    Write-Host "JAR file not found. Build may have failed." -ForegroundColor Red
}

Write-Host "Press any key to continue..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
