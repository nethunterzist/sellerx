# JWT Secret Generator for Railway Deployment
# Run this script to generate a secure JWT secret

Write-Host "JWT Secret Generator for SellerX Railway Deployment" -ForegroundColor Green
Write-Host "=================================================" -ForegroundColor Green

# Generate a secure random JWT secret
$bytes = New-Object byte[] 32
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
$jwtSecret = [System.Convert]::ToBase64String($bytes)

Write-Host ""
Write-Host "Generated JWT Secret:" -ForegroundColor Yellow
Write-Host $jwtSecret -ForegroundColor Cyan
Write-Host ""
Write-Host "Instructions:" -ForegroundColor Green
Write-Host "1. Copy the JWT secret above" -ForegroundColor White
Write-Host "2. Go to your Railway project dashboard" -ForegroundColor White
Write-Host "3. Select your backend service" -ForegroundColor White
Write-Host "4. Go to Variables tab" -ForegroundColor White
Write-Host "5. Add new variable: JWT_SECRET = [paste the secret]" -ForegroundColor White
Write-Host ""
Write-Host "IMPORTANT: Keep this secret secure and don't share it!" -ForegroundColor Red

# Copy to clipboard if possible
try {
    $jwtSecret | Set-Clipboard
    Write-Host "JWT Secret has been copied to clipboard!" -ForegroundColor Green
} catch {
    Write-Host "Could not copy to clipboard. Please copy manually." -ForegroundColor Yellow
}
