[Setup]
AppId={{3E658137-CE80-49E3-8084-FD0B0158CA31}
AppName="NoteBot"
AppVersion="1.5"
AppPublisher="Federico Dossena"
AppPublisherURL="http://notebot.fdossena.com/"
AppSupportURL="http://notebot.fdossena.com/"
AppUpdatesURL="http://notebot.fdossena.com/"
DefaultDirName="{pf}\NoteBot"
DefaultGroupName="NoteBot"
DisableProgramGroupPage=yes
LicenseFile=../LICENSE
OutputDir=../build/libs/Windows
OutputBaseFilename=notebot-setup
SetupIconFile="../src/main/resources/icon.ico"
UninstallDisplayIcon="../src/main/resources/icon.ico"

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"
Name: "italian"; MessagesFile: "compiler:Languages\Italian.isl"

[Files]
; Source: "../build/libs/StickyNotes.jar"; DestDir: "{app}"; Destname: "StickyNotes.exe";
Source: "../build/libs/StickyNotes-windows.exe"; DestDir: "{app}";
Source: "windows-jre/*"; DestDir: "{app}/runtime"; Flags: ignoreversion recursesubdirs createallsubdirs sortfilesbyextension;
  
[Icons]
Name: "{group}\NoteBot"; Filename: "{app}\StickyNotes.exe"

[Run]
Filename:"{app}\StickyNotes.exe"; Flags:runasoriginaluser nowait;

[Registry]
Root: HKLM; Subkey: "SOFTWARE\Microsoft\Windows\CurrentVersion\Run"; ValueName: "NoteBot"; ValueType: string; ValueData: """{app}\StickyNotes.exe"" -autostartup"; Flags: uninsdeletevalue 
Root: HKLM32; Subkey: "SOFTWARE\Microsoft\Windows\CurrentVersion\Run"; ValueName: "NoteBot"; ValueType: string; ValueData: """{app}\StickyNotes.exe"" -autostartup"; Flags: uninsdeletevalue 
