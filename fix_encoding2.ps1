$files = Get-ChildItem "C:\Users\ia\AndroidStudioProjects\bocatta\app\src\main\java\com\bocatta\pos\presentation\viewmodel\*.kt"
foreach ($f in $files) {
    $content = [System.IO.File]::ReadAllText($f.FullName)
    $orig = $content
    $content = $content.Replace([char]0xFFFD, [char]0x00D1)
    if ($content -ne $orig) {
        [System.IO.File]::WriteAllText($f.FullName, $content, [System.Text.UTF8Encoding]::new($false))
        Write-Output ("Fixed: " + $f.Name)
    }
}
