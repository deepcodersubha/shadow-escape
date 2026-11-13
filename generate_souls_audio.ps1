function Write-Wav([string]$path, [float[]]$samples, [int]$sampleRate = 22050) {
    $stream = [System.IO.File]::OpenWrite($path)
    $writer = New-Object System.IO.BinaryWriter($stream)
    
    $byteRate = $sampleRate * 2
    $dataLength = $samples.Length * 2
    
    # RIFF header
    $writer.Write([System.Text.Encoding]::ASCII.GetBytes("RIFF"))
    $writer.Write([int]($dataLength + 36))
    $writer.Write([System.Text.Encoding]::ASCII.GetBytes("WAVE"))
    
    # fmt chunk
    $writer.Write([System.Text.Encoding]::ASCII.GetBytes("fmt "))
    $writer.Write([int]16)       # Subchunk1Size
    $writer.Write([int16]1)      # AudioFormat (PCM = 1)
    $writer.Write([int16]1)      # NumChannels (Mono = 1)
    $writer.Write([int]$sampleRate)
    $writer.Write([int]$byteRate)
    $writer.Write([int16]2)      # BlockAlign
    $writer.Write([int16]16)     # BitsPerSample
    
    # data chunk
    $writer.Write([System.Text.Encoding]::ASCII.GetBytes("data"))
    $writer.Write([int]$dataLength)
    
    foreach ($s in $samples) {
        $clamped = [Math]::Max(-1.0, [Math]::Min(1.0, $s))
        $val = [int16]($clamped * 32767.0)
        $writer.Write($val)
    }
    
    $writer.Close()
    $stream.Close()
}

$sampleRate = 22050
$audioDir = "C:\Users\dell\AndroidStudioProjects\ShadowEscape2\app\src\main\assets\audio"

# 1. visceral_strike.wav (0.55s)
$dur1 = 0.55
$len1 = [int]($sampleRate * $dur1)
$s1 = New-Object float[] $len1
$rand = New-Object System.Random
for ($i = 0; $i -lt $len1; $i++) {
    $t = $i / $sampleRate
    $sub = [Math]::Sin(2 * [Math]::PI * 55 * $t) * [Math]::Exp(-$t * 8.0)
    $chime = ([Math]::Sin(2 * [Math]::PI * 720 * $t) * 0.5 + [Math]::Sin(2 * [Math]::PI * 1440 * $t) * 0.3) * [Math]::Exp(-$t * 6.0)
    $noise = ($rand.NextDouble() * 2.0 - 1.0) * [Math]::Exp(-$t * 28.0)
    $gong = [Math]::Sin(2 * [Math]::PI * 180 * $t) * [Math]::Exp(-$t * 4.0) * 0.4
    $s1[$i] = ($sub * 0.6 + $chime * 0.5 + $noise * 0.4 + $gong * 0.3)
}
Write-Wav "$audioDir\visceral_strike.wav" $s1 $sampleRate

# 2. bloodstain_reclaim.wav (0.65s)
$dur2 = 0.65
$len2 = [int]($sampleRate * $dur2)
$s2 = New-Object float[] $len2
for ($i = 0; $i -lt $len2; $i++) {
    $t = $i / $sampleRate
    $c5 = [Math]::Sin(2 * [Math]::PI * 523.25 * $t) * [Math]::Exp(-$t * 3.5)
    $e5 = $(if ($t -ge 0.08) { [Math]::Sin(2 * [Math]::PI * 659.25 * $t) * [Math]::Exp(-($t - 0.08) * 3.5) } else { 0.0 })
    $g5 = $(if ($t -ge 0.16) { [Math]::Sin(2 * [Math]::PI * 783.99 * $t) * [Math]::Exp(-($t - 0.16) * 3.5) } else { 0.0 })
    $c6 = $(if ($t -ge 0.24) { [Math]::Sin(2 * [Math]::PI * 1046.50 * $t) * [Math]::Exp(-($t - 0.24) * 3.2) } else { 0.0 })
    $tremolo = 0.8 + 0.2 * [Math]::Sin(2 * [Math]::PI * 12 * $t)
    $s2[$i] = ($c5 * 0.3 + $e5 * 0.35 + $g5 * 0.35 + $c6 * 0.4) * $tremolo
}
Write-Wav "$audioDir\bloodstain_reclaim.wav" $s2 $sampleRate

# 3. mimic_growl.wav (0.45s)
$dur3 = 0.45
$len3 = [int]($sampleRate * $dur3)
$s3 = New-Object float[] $len3
for ($i = 0; $i -lt $len3; $i++) {
    $t = $i / $sampleRate
    $snap = ($rand.NextDouble() * 2.0 - 1.0) * [Math]::Exp(-$t * 35.0)
    $snarl = [Math]::Sin(2 * [Math]::PI * 85 * $t + [Math]::Sin(2 * [Math]::PI * 25 * $t) * 2.5) * [Math]::Exp(-$t * 4.5)
    $s3[$i] = ($snap * 0.5 + $snarl * 0.6)
}
Write-Wav "$audioDir\mimic_growl.wav" $s3 $sampleRate
Write-Host "Audio files successfully generated!"
