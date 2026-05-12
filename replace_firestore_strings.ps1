$pattern = '"FirestoreCollections\.([A-Z_]+)"'
Get-ChildItem -Path "C:/Users/ia/AndroidStudioProjects/bocatta/app/src/main/java" -Recurse -Filter *.kt | ForEach-Object {
    $content = Get-Content $_.FullName -Raw
    $newContent = [regex]::Replace($content, $pattern, 'FirestoreCollections.$1')
    if ($newContent -ne $content) {
        Set-Content -Path $_.FullName -Value $newContent -Encoding utf8
    }
}
