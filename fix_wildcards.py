import os, re
import subprocess

packages = {
    'androidx.compose.runtime': [
        'Composable', 'remember', 'mutableStateOf', 'mutableIntStateOf', 'mutableDoubleStateOf', 'mutableFloatStateOf',
        'LaunchedEffect', 'SideEffect', 'DisposableEffect', 'rememberCoroutineScope', 'State', 'MutableState',
        'getValue', 'setValue', 'derivedStateOf', 'snapshotFlow', 'produceState', 'CompositionLocalProvider',
        'staticCompositionLocalOf', 'compositionLocalOf', 'collectAsState', 'rememberUpdatedState', 'mutableStateListOf', 'mutableStateMapOf', 'MutableStateList'
    ],
    'androidx.compose.foundation.layout': [
        'Column', 'Row', 'Box', 'Spacer', 'fillMaxSize', 'fillMaxWidth', 'fillMaxHeight', 'padding', 'size',
        'width', 'height', 'Arrangement', 'Alignment', 'offset', 'aspectRatio', 'wrapContentSize', 'Spacer',
        'BoxWithConstraints', 'IntrinsicSize', 'WindowInsets', 'imePadding', 'systemBarsPadding', 'ColumnScope', 'RowScope', 'BoxScope'
    ],
    'androidx.compose.ui': [
        'Modifier', 'Alignment', 'graphics', 'drawBehind', 'text', 'unit', 'layout', 'platform', 'ExperimentalComposeUiApi', 'focus'
    ],
    'androidx.compose.material3': [
        'Text', 'Button', 'Icon', 'IconButton', 'Card', 'Surface', 'MaterialTheme', 'Scaffold', 'TopAppBar',
        'BottomAppBar', 'FloatingActionButton', 'Divider', 'Snackbar', 'SnackbarHost', 'SnackbarHostState',
        'AlertDialog', 'TextField', 'OutlinedTextField', 'Checkbox', 'RadioButton', 'Switch', 'Slider',
        'CircularProgressIndicator', 'LinearProgressIndicator', 'Badge', 'BadgedBox', 'NavigationBar',
        'NavigationBarItem', 'NavigationRail', 'NavigationRailItem', 'ModalDrawerSheet', 'ModalNavigationDrawer',
        'DrawerValue', 'rememberDrawerState', 'ButtonDefaults', 'CardDefaults', 'ElevatedCard', 'OutlinedCard',
        'ListItem', 'DropdownMenu', 'DropdownMenuItem', 'ExposedDropdownMenuBox', 'TextButton', 'OutlinedButton',
        'LocalContentColor', 'LocalTextStyle', 'FilterChip', 'ElevatedFilterChip', 'AssistChip', 'ElevatedAssistChip', 'ExperimentalMaterial3Api'
    ],
    'com.bocatta.pos.domain.model': [
        'Venta', 'VentaV2', 'Producto', 'Categoria', 'Sucursal', 'Turno', 'Caja', 'Usuario', 'Rol', 'Gasto',
        'Merma', 'Stock', 'Insumo', 'Proveedor', 'Pedido', 'Inventario', 'Apertura', 'Cierre', 'Ticket', 'Orden',
        'OrdenActiva', 'OrdenActivaV2', 'Cliente', 'ClienteV2', 'Promocion', 'Descuento', 'Impuesto', 'MetodoPago',
        'TipoPago', 'FormaPago', 'Permiso', 'Zona', 'Empleado', 'Asistencia', 'Sueldo', 'Comision', 'Historial'
    ],
    'androidx.compose.foundation': [
        'background', 'border', 'clickable', 'shape', 'interaction', 'lazy', 'rememberScrollState', 'verticalScroll', 'horizontalScroll',
        'Image', 'Canvas', 'ScrollState', 'Indication', 'LocalIndication', 'BorderStroke'
    ],
    'org.junit.Assert': [
        'assertEquals', 'assertNotEquals', 'assertTrue', 'assertFalse', 'assertNull', 'assertNotNull', 'assertSame', 'assertNotSame', 'fail', 'assertThrows'
    ]
}

count = 0
for root, dirs, files in os.walk('app/src'):
    for name in files:
        if name.endswith('.kt'):
            path = os.path.join(root, name)
            with open(path, 'r', encoding='utf-8') as f:
                content = f.read()
            
            new_content = content
            
            for pkg, classes in packages.items():
                wildcard = f'import {pkg}.*'
                if wildcard in new_content:
                    # Find which classes are actually used
                    used_classes = []
                    for cls in classes:
                        # naive word boundary match
                        if re.search(r'\b' + cls + r'\b', new_content):
                            used_classes.append(cls)
                    
                    if used_classes:
                        explicit_imports = '\n'.join([f'import {pkg}.{cls}' for cls in sorted(used_classes)])
                        new_content = new_content.replace(wildcard, explicit_imports)
                    else:
                        new_content = new_content.replace(wildcard + '\n', '')
            
            if content != new_content:
                with open(path, 'w', encoding='utf-8') as f:
                    f.write(new_content)
                count += 1
print('Processed', count, 'files for wildcard imports.')
