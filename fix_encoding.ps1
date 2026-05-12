$files = Get-ChildItem "C:\Users\ia\AndroidStudioProjects\bocatta\app\src\main\java\com\bocatta\pos\presentation\viewmodel\*.kt"
foreach ($file in $files) {
    $content = Get-Content $file.FullName -Raw -Encoding UTF8
    if ($content -match 'DUE.O') {
        $content = $content -replace 'DUE.O', 'DUEÑO'
        [System.IO.File]::WriteAllText($file.FullName, $content, [System.Text.UTF8Encoding]::new($false))
        Write-Output "Fixed: $($file.Name)"
    }
}
