$loginUrl = 'http://localhost:8080/login'
$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$pre = Invoke-WebRequest -Uri $loginUrl -WebSession $session -UseBasicParsing
$csrf = ''
if ($pre.Content -match 'name="_csrf"\s+value="([^"]+)"') { $csrf = $Matches[1] }
Write-Host "CSRF: $csrf"
$body = @{
    email    = 'admin@taller.local'
    password = 'AdminTallerMecanico2026!'
}
if ($csrf) { $body._csrf = $csrf }
try {
    $loginResp = Invoke-WebRequest -Uri $loginUrl -Method Post -WebSession $session -Body $body -UseBasicParsing -MaximumRedirection 0
    Write-Host "Login status: $($loginResp.StatusCode)"
} catch {
    Write-Host "Login redirect (expected): Status=$($_.Exception.Response.StatusCode.value__)"
}
Write-Host "=== Prueba /dashboard ==="
$d = Invoke-WebRequest -Uri 'http://localhost:8080/dashboard' -WebSession $session -UseBasicParsing
$tituloOk = [bool]($d.Content -match 'Panel principal')
$statsOk  = [bool](($d.Content -match 'Usuarios registrados') -and ($d.Content -match 'Total de citas'))
$nohardOk = -not [bool]($d.Content -match 'Usuarios de prueba|admin@taller.com|Admin12345678|12 citas|7 vehiculos|48\.5k')
Write-Host "Status: $($d.StatusCode) | Title: $tituloOk | Stats reales: $statsOk | Sin hardcodes: $nohardOk"
Write-Host "=== Prueba /usuarios ==="
$u = Invoke-WebRequest -Uri 'http://localhost:8080/usuarios' -WebSession $session -UseBasicParsing
$titleOk  = [bool]($u.Content -match 'Usuarios y roles')
$adminRow = [bool]($u.Content -match 'Administrador')
$roleSel  = [bool]($u.Content -match 'ADMINISTRADOR')
$apiOk    = [bool]($u.Content -match '/api/admin/users/')
Write-Host "Status: $($u.StatusCode) | Title: $titleOk | Admin row: $adminRow | Role select: $roleSel | API wired: $apiOk"
Write-Host "=== Prueba /citas ==="
$c = Invoke-WebRequest -Uri 'http://localhost:8080/citas' -WebSession $session -UseBasicParsing
$cTitle = [bool]($c.Content -match 'Citas \| Taller Norte')
$cForm  = [bool]($c.Content -match 'Agenda de citas')
$cEmpty = [bool]($c.Content -match 'No hay citas registradas')
$cEst   = [bool]($c.Content -match 'PENDIENTE')
$cApi   = [bool]($c.Content -match '/api/citas/')
Write-Host "Status: $($c.StatusCode) | Title: $cTitle | Form: $cForm | Empty state: $cEmpty | Estados: $cEst | API wired: $cApi"
Write-Host ""
Write-Host "=== Prueba API Crear Cita (POST /api/citas) ==="
$citaBody = @{
    nombreCliente = 'Juan Pérez'
    telefono      = '5512345678'
    email         = 'juan@ejemplo.com'
    vehiculo      = 'Nissan Versa 2022'
    placa         = 'ABC-123'
    fecha         = (Get-Date).AddDays(1).ToString('yyyy-MM-dd')
    hora          = '10:00'
    servicio      = 'Cambio de aceite y filtros'
    notas         = 'Llantas traseras con desgaste'
} | ConvertTo-Json
$citaResp = Invoke-RestMethod -Uri 'http://localhost:8080/api/citas' -Method Post -WebSession $session -Body $citaBody -ContentType 'application/json'
Write-Host "Cita creada: id=$($citaResp.id) cliente=$($citaResp.nombreCliente) estado=$($citaResp.estado)"
Write-Host "=== Prueba API Listar citas (GET /api/citas) ==="
$lista = Invoke-RestMethod -Uri 'http://localhost:8080/api/citas' -Method Get -WebSession $session
Write-Host "Total citas listadas: $($lista.Count) | 1ra placa: $($lista[0].placa)"
Write-Host "=== Prueba Cambiar estado (PATCH /api/citas/$($citaResp.id)/estado) ==="
$estadoBody = @{ estado = 'CONFIRMADA' } | ConvertTo-Json
$estResp = Invoke-RestMethod -Uri "http://localhost:8080/api/citas/$($citaResp.id)/estado" -Method Patch -WebSession $session -Body $estadoBody -ContentType 'application/json'
Write-Host "Nuevo estado: $($estResp.estado)"
Write-Host "=== Prueba Eliminar cita (DELETE /api/citas/$($citaResp.id)) ==="
$delResp = Invoke-WebRequest -Uri "http://localhost:8080/api/citas/$($citaResp.id)" -Method Delete -WebSession $session -UseBasicParsing
Write-Host "Delete status: $($delResp.StatusCode)"
$lista2 = Invoke-RestMethod -Uri 'http://localhost:8080/api/citas' -Method Get -WebSession $session
Write-Host "Citas restantes: $($lista2.Count)"
Write-Host ""
Write-Host "=== Prueba Registro cliente (API auth/register) ==="
$regBody = @{
    email    = 'recepcionista@prueba.local'
    fullName = 'Recepcionista Prueba'
    password = 'Recep123456789!'
} | ConvertTo-Json
$regResp = Invoke-RestMethod -Uri 'http://localhost:8080/api/auth/register' -Method Post -Body $regBody -ContentType 'application/json'
Write-Host "Registro OK: $($regResp.message)"
$usersAntes = (Invoke-RestMethod -Uri 'http://localhost:8080/api/admin/users' -Method Get -WebSession $session).Count
Write-Host "Usuarios totales antes cambio rol: $usersAntes"
$nuevoId = (Invoke-RestMethod -Uri 'http://localhost:8080/api/admin/users' -Method Get -WebSession $session | Where-Object { $_.email -eq 'recepcionista@prueba.local' }).id
Write-Host "ID nuevo usuario: $nuevoId"
Write-Host "=== Prueba Cambiar rol usuario (PATCH /api/admin/users/$nuevoId/role) ==="
$rolBody = @{ role = 'RECEPCIONISTA' } | ConvertTo-Json
$rolResp = Invoke-RestMethod -Uri "http://localhost:8080/api/admin/users/$nuevoId/role" -Method Patch -WebSession $session -Body $rolBody -ContentType 'application/json'
Write-Host "Rol cambiado: $($rolResp.nuevoRol) | email=$($rolResp.email)"
Write-Host ""
Write-Host "=== REFRESH /usuarios y /citas (despues de acciones) ==="
$u2 = Invoke-WebRequest -Uri 'http://localhost:8080/usuarios' -WebSession $session -UseBasicParsing
$rows = [regex]::Matches($u2.Content, '<tr\b').Count
Write-Host "/usuarios TR rows: $rows (2 rows = thead + admin, al menos 3 con recep)"
$c2 = Invoke-WebRequest -Uri 'http://localhost:8080/citas' -WebSession $session -UseBasicParsing
$c2RenderOk = [bool]($c2.Content -match 'Agenda de citas')
Write-Host "/citas sigue renderizando OK: $c2RenderOk"
Write-Host ""
Write-Host "DONE."
