$java = "C:/Users/ia/AndroidStudioProjects/bocatta/app/src/main/java"
$pairs = @(
    "v2_ventas=FirestoreCollections.VENTAS",
    "v2_products=FirestoreCollections.PRODUCTOS",
    "v2_gastos=FirestoreCollections.GASTOS",
    "v2_users=FirestoreCollections.USUARIOS",
    "v2_customers=FirestoreCollections.CLIENTES",
    "v2_inventory_global=FirestoreCollections.INVENTARIO_GLOBAL",
    "v2_inventory_branch_portions=FirestoreCollections.INVENTARIO_SUCURSAL",
    "v2_inventory_movements=FirestoreCollections.MOVIMIENTOS_INVENTARIO",
    "v2_recetas=FirestoreCollections.RECETAS",
    "v2_auditoria_cancelaciones=FirestoreCollections.CANCELACIONES",
    "v2_devoluciones=FirestoreCollections.DEVOLUCIONES",
    "v2_turnos_caja=FirestoreCollections.TURNOS_CAJA",
    "v2_supplies=FirestoreCollections.INSUMOS",
    "v2_merma_logs=FirestoreCollections.MERMA_LOGS",
    "v2_configuracion=FirestoreCollections.CONFIGURACION",
    "v2_sucursal_config=FirestoreCollections.SUCURSAL_CONFIG",
    "v2_purchase_history=FirestoreCollections.COMPRAS"
)
Get-ChildItem -Path $java -Recurse -Filter *.kt | Where-Object { $_.FullName -notlike "*FirestoreCollections.kt" } | ForEach-Object {
    $text = Get-Content $_.FullName -Raw
    foreach ($pair in $pairs) {
        $kv = $pair -split "="
        $old = $kv[0]
        $new = $kv[1]
        $text = $text -replace [regex]::Escape($old), $new
    }
    Set-Content -Path $_.FullName -Value $text -Encoding utf8
}