$ErrorActionPreference = 'Stop'

Add-Type -AssemblyName System.Drawing

$root = Split-Path -Parent $PSScriptRoot
$resourceRoot = Join-Path $root 'src\main\resources'

function Ensure-Directory([string]$path) {
    if (-not (Test-Path -LiteralPath $path)) {
        New-Item -ItemType Directory -Path $path -Force | Out-Null
    }
}

function Save-Png([System.Drawing.Bitmap]$bitmap, [string]$path) {
    Ensure-Directory (Split-Path -Parent $path)
    $bitmap.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $bitmap.Dispose()
}

function Lerp([double]$a, [double]$b, [double]$amount) {
    return $a + (($b - $a) * $amount)
}

function New-RockTexture([int]$size, [int]$seed, [int]$crackCount, [int]$glowAlpha, [bool]$transparent = $false) {
    $bitmap = New-Object System.Drawing.Bitmap($size, $size, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $random = New-Object System.Random($seed)

    for ($y = 0; $y -lt $size; $y++) {
        for ($x = 0; $x -lt $size; $x++) {
            $u = $x / [double]$size
            $v = $y / [double]$size
            $fine = [math]::Sin(($x + $seed) * 0.77) * [math]::Cos(($y - $seed) * 0.61)
            $broad = [math]::Sin(($x * 0.11) + ($y * 0.16) + $seed) + [math]::Cos(($x * 0.19) - ($y * 0.09) - $seed)
            $grain = (($fine + 1.0) * 0.5 * 0.55) + (($broad + 2.0) * 0.25 * 0.45)
            $edge = 1.0 - (0.16 * [math]::Max([math]::Abs($u - 0.5), [math]::Abs($v - 0.5)) * 2.0)
            $r = [int][math]::Max(18, [math]::Min(130, 48 + (52 * $grain) + (8 * $edge)))
            $g = [int][math]::Max(14, [math]::Min(104, 35 + (38 * $grain) + (7 * $edge)))
            $b = [int][math]::Max(10, [math]::Min(72, 24 + (24 * $grain) + (4 * $edge)))
            $alpha = if ($transparent) { 255 } else { 255 }
            $bitmap.SetPixel($x, $y, [System.Drawing.Color]::FromArgb($alpha, $r, $g, $b))
        }
    }

    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $graphics.CompositingMode = [System.Drawing.Drawing2D.CompositingMode]::SourceOver

    # Layered mineral plates add a hand-carved, faceted silhouette to the procedural stone.
    $plateBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(36, 16, 12, 9))
    for ($i = 0; $i -lt [math]::Max(5, [int]($size / 8)); $i++) {
        $px = $random.Next(-$size / 4, $size)
        $py = $random.Next(-$size / 4, $size)
        $pw = $random.Next([int]($size / 8), [int]($size / 2))
        $ph = $random.Next([int]($size / 10), [int]($size / 3))
        $points = @(
            [System.Drawing.PointF]::new($px, $py + ($ph * 0.35)),
            [System.Drawing.PointF]::new($px + ($pw * 0.78), $py),
            [System.Drawing.PointF]::new($px + $pw, $py + ($ph * 0.62)),
            [System.Drawing.PointF]::new($px + ($pw * 0.36), $py + $ph),
            [System.Drawing.PointF]::new($px, $py + ($ph * 0.35))
        )
        $graphics.FillPolygon($plateBrush, $points)
    }
    $plateBrush.Dispose()

    # Amber veins grow denser and brighter at each awakening stage.
    $darkPen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(190, 83, 35, 8), [math]::Max(1.4, $size / 45.0))
    $glowPen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb([int]($glowAlpha * 0.35), 255, 104, 16), [math]::Max(3.0, $size / 24.0))
    $brightPen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb($glowAlpha, 255, 196, 56), [math]::Max(0.8, $size / 70.0))
    for ($i = 0; $i -lt $crackCount; $i++) {
        $x = [double]$random.Next(0, $size)
        $y = [double]$random.Next(0, $size)
        $angle = $random.NextDouble() * [math]::PI * 2.0
        $length = $random.Next([int]($size * 0.22), [int]($size * 0.72))
        $points = @([System.Drawing.PointF]::new($x, $y))
        $segments = $random.Next(3, 7)
        for ($segment = 0; $segment -lt $segments; $segment++) {
            $angle += ($random.NextDouble() - 0.5) * 1.1
            $step = $length / $segments
            $x += [math]::Cos($angle) * $step
            $y += [math]::Sin($angle) * $step
            $points += [System.Drawing.PointF]::new([float]$x, [float]$y)
        }
        $pointArray = [System.Drawing.PointF[]]$points
        $graphics.DrawLines($darkPen, $pointArray)
        $graphics.DrawLines($glowPen, $pointArray)
        $graphics.DrawLines($brightPen, $pointArray)
        if ($random.NextDouble() -gt 0.35) {
            $branch = $points[$random.Next(1, $points.Count - 1)]
            $branchPoints = @($branch)
            $branchAngle = $angle + (($random.NextDouble() - 0.5) * [math]::PI)
            $branchLength = $length * 0.32
            for ($branchSegment = 0; $branchSegment -lt 3; $branchSegment++) {
                $branchX = $branch.X + ([math]::Cos($branchAngle) * $branchLength * (($branchSegment + 1) / 3.0))
                $branchY = $branch.Y + ([math]::Sin($branchAngle) * $branchLength * (($branchSegment + 1) / 3.0))
                $branchPoints += [System.Drawing.PointF]::new([float]$branchX, [float]$branchY)
            }
            $branchArray = [System.Drawing.PointF[]]$branchPoints
            $graphics.DrawLines($glowPen, $branchArray)
            $graphics.DrawLines($brightPen, $branchArray)
        }
    }
    $darkPen.Dispose()
    $glowPen.Dispose()
    $brightPen.Dispose()

    # Small amber inclusions make the texture readable at Minecraft scale.
    for ($i = 0; $i -lt [math]::Max(10, $size / 5); $i++) {
        $fx = $random.Next(1, $size - 2)
        $fy = $random.Next(1, $size - 2)
        $radius = $random.Next(1, [math]::Max(2, [int]($size / 18)))
        $brush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb([int]($glowAlpha * 0.65), 239, 128, 25))
        $graphics.FillEllipse($brush, $fx - $radius, $fy - $radius, $radius * 2, $radius * 2)
        $brush.Dispose()
    }

    $graphics.Dispose()
    return $bitmap
}

function New-TitanTexture([int]$seed) {
    $bitmap = New-RockTexture 128 $seed 34 245
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias

    $silhouette = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(125, 19, 17, 14))
    $shoulders = @(
        [System.Drawing.PointF]::new(16, 45), [System.Drawing.PointF]::new(33, 31),
        [System.Drawing.PointF]::new(49, 35), [System.Drawing.PointF]::new(64, 29),
        [System.Drawing.PointF]::new(79, 35), [System.Drawing.PointF]::new(95, 31),
        [System.Drawing.PointF]::new(112, 47), [System.Drawing.PointF]::new(101, 74),
        [System.Drawing.PointF]::new(88, 68), [System.Drawing.PointF]::new(85, 112),
        [System.Drawing.PointF]::new(43, 112), [System.Drawing.PointF]::new(40, 68),
        [System.Drawing.PointF]::new(26, 75)
    )
    $graphics.FillPolygon($silhouette, $shoulders)
    $head = @(
        [System.Drawing.PointF]::new(47, 20), [System.Drawing.PointF]::new(64, 11),
        [System.Drawing.PointF]::new(81, 21), [System.Drawing.PointF]::new(84, 43),
        [System.Drawing.PointF]::new(68, 57), [System.Drawing.PointF]::new(48, 43)
    )
    $graphics.FillPolygon($silhouette, $head)
    $silhouette.Dispose()

    $outline = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(210, 110, 52, 13), 1.2)
    $graphics.DrawPolygon($outline, $shoulders)
    $graphics.DrawPolygon($outline, $head)
    $outline.Dispose()

    $eyeGlow = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 255, 190, 48))
    $eyeHot = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 255, 245, 178))
    $graphics.FillPolygon($eyeGlow, @([System.Drawing.PointF]::new(52, 29), [System.Drawing.PointF]::new(62, 28), [System.Drawing.PointF]::new(57, 34)))
    $graphics.FillPolygon($eyeGlow, @([System.Drawing.PointF]::new(68, 28), [System.Drawing.PointF]::new(78, 29), [System.Drawing.PointF]::new(73, 34)))
    $graphics.FillEllipse($eyeHot, 56, 29, 3, 3)
    $graphics.FillEllipse($eyeHot, 71, 29, 3, 3)
    $eyeGlow.Dispose()
    $eyeHot.Dispose()

    $chestPen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(240, 255, 156, 27), 1.5)
    $graphics.DrawLine($chestPen, 64, 53, 64, 99)
    $graphics.DrawLine($chestPen, 64, 64, 52, 76)
    $graphics.DrawLine($chestPen, 64, 64, 76, 76)
    $graphics.DrawLine($chestPen, 52, 76, 45, 93)
    $graphics.DrawLine($chestPen, 76, 76, 84, 93)
    $chestPen.Dispose()
    $graphics.Dispose()
    return $bitmap
}

function New-SwordTexture {
    $bitmap = New-Object System.Drawing.Bitmap(64, 64, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $bladeGlow = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(150, 255, 151, 20), 6)
    $bladeEdge = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(255, 218, 157, 61), 2)
    $blade = @(
        [System.Drawing.PointF]::new(13, 49), [System.Drawing.PointF]::new(19, 43),
        [System.Drawing.PointF]::new(43, 9), [System.Drawing.PointF]::new(50, 5),
        [System.Drawing.PointF]::new(47, 16), [System.Drawing.PointF]::new(22, 48)
    )
    $graphics.DrawPolygon($bladeGlow, $blade)
    $graphics.FillPolygon((New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 96, 75, 47))), $blade)
    $graphics.DrawPolygon($bladeEdge, $blade)
    $crackPen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(255, 255, 205, 71), 1.5)
    $graphics.DrawLine($crackPen, 24, 40, 29, 34)
    $graphics.DrawLine($crackPen, 29, 34, 27, 28)
    $graphics.DrawLine($crackPen, 34, 29, 38, 22)
    $graphics.DrawLine($crackPen, 39, 21, 37, 16)
    $crackPen.Dispose()
    $guardPen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(255, 188, 110, 27), 4)
    $graphics.DrawLine($guardPen, 10, 47, 28, 57)
    $graphics.DrawLine($guardPen, 17, 42, 33, 51)
    $guardPen.Dispose()
    $handle = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(255, 55, 39, 26), 6)
    $graphics.DrawLine($handle, 20, 51, 12, 60)
    $pommel = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 224, 148, 37))
    $graphics.FillEllipse($pommel, 8, 56, 8, 8)
    $pommel.Dispose()
    $handle.Dispose()
    $bladeGlow.Dispose()
    $bladeEdge.Dispose()
    $graphics.Dispose()
    return $bitmap
}

function New-PickaxeTexture {
    $bitmap = New-Object System.Drawing.Bitmap(32, 32, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $headShape = @(
        [System.Drawing.PointF]::new(4, 10), [System.Drawing.PointF]::new(9, 5),
        [System.Drawing.PointF]::new(16, 3), [System.Drawing.PointF]::new(27, 6),
        [System.Drawing.PointF]::new(29, 10), [System.Drawing.PointF]::new(23, 12),
        [System.Drawing.PointF]::new(17, 9), [System.Drawing.PointF]::new(12, 14),
        [System.Drawing.PointF]::new(6, 14)
    )
    $headGlow = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(150, 255, 145, 18), 5)
    $headEdge = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(255, 224, 157, 55), 1.5)
    $headBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 83, 64, 42))
    $graphics.DrawPolygon($headGlow, $headShape)
    $graphics.FillPolygon($headBrush, $headShape)
    $graphics.DrawPolygon($headEdge, $headShape)
    $headGlow.Dispose()
    $headEdge.Dispose()
    $headBrush.Dispose()
    $handleGlow = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(120, 255, 132, 14), 5)
    $handle = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(255, 83, 48, 24), 3)
    $graphics.DrawLine($handleGlow, 13, 10, 24, 28)
    $graphics.DrawLine($handle, 13, 10, 24, 28)
    $handleGlow.Dispose()
    $handle.Dispose()
    $crack = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(255, 255, 205, 73), 1)
    $graphics.DrawLine($crack, 9, 8, 14, 7)
    $graphics.DrawLine($crack, 14, 7, 16, 4)
    $graphics.DrawLine($crack, 19, 7, 22, 10)
    $graphics.DrawLine($crack, 22, 10, 25, 8)
    $crack.Dispose()
    $graphics.Dispose()
    return $bitmap
}

function Resize-Bitmap([System.Drawing.Bitmap]$source, [int]$size) {
    $target = New-Object System.Drawing.Bitmap($size, $size, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $graphics = [System.Drawing.Graphics]::FromImage($target)
    $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    $graphics.DrawImage($source, [System.Drawing.Rectangle]::new(0, 0, $size, $size))
    $graphics.Dispose()
    $source.Dispose()
    return $target
}

function New-CoreLogo {
    $bitmap = New-Object System.Drawing.Bitmap(128, 128, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $graphics.Clear([System.Drawing.Color]::FromArgb(255, 17, 13, 12))
    $shadow = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(150, 0, 0, 0))
    $graphics.FillEllipse($shadow, 12, 88, 105, 25)
    $shadow.Dispose()
    $rock = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 71, 48, 28))
    $rockPoints = @(
        [System.Drawing.PointF]::new(22, 66), [System.Drawing.PointF]::new(29, 30),
        [System.Drawing.PointF]::new(62, 13), [System.Drawing.PointF]::new(98, 34),
        [System.Drawing.PointF]::new(105, 71), [System.Drawing.PointF]::new(75, 100),
        [System.Drawing.PointF]::new(40, 94)
    )
    $graphics.FillPolygon($rock, $rockPoints)
    $rock.Dispose()
    $edge = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(255, 159, 83, 21), 3)
    $graphics.DrawPolygon($edge, $rockPoints)
    $edge.Dispose()
    $glow = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(220, 255, 190, 41), 4)
    $graphics.DrawLine($glow, 62, 18, 57, 48)
    $graphics.DrawLine($glow, 57, 48, 39, 70)
    $graphics.DrawLine($glow, 57, 48, 78, 62)
    $graphics.DrawLine($glow, 78, 62, 87, 86)
    $graphics.DrawLine($glow, 57, 48, 68, 29)
    $glow.Dispose()
    $font = New-Object System.Drawing.Font('Segoe UI', 14, [System.Drawing.FontStyle]::Bold, [System.Drawing.GraphicsUnit]::Pixel)
    $textBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 255, 225, 137))
    $graphics.DrawString('EARTH', $font, $textBrush, 24, 103)
    $textBrush.Dispose()
    $font.Dispose()
    $graphics.Dispose()
    return $bitmap
}

Ensure-Directory (Join-Path $resourceRoot 'assets\earthtitan\textures\block')
Ensure-Directory (Join-Path $resourceRoot 'assets\earthtitan\textures\item')
Ensure-Directory (Join-Path $resourceRoot 'assets\earthtitan\textures\entity')

Save-Png (New-RockTexture 64 1201 18 235) (Join-Path $resourceRoot 'assets\earthtitan\textures\block\earth_titan_core.png')
Save-Png (New-PickaxeTexture) (Join-Path $resourceRoot 'assets\earthtitan\textures\item\earth_pickaxe.png')
for ($stage = 1; $stage -le 6; $stage++) {
    $cracks = 5 + ($stage * 5)
    $alpha = 125 + ($stage * 20)
    Save-Png (New-RockTexture 128 (3000 + $stage) $cracks $alpha) (Join-Path $resourceRoot ("assets\earthtitan\textures\entity\titan_stage_{0}.png" -f $stage))
}
Save-Png (New-TitanTexture 4096) (Join-Path $resourceRoot 'assets\earthtitan\textures\entity\titan.png')
Save-Png (New-SwordTexture) (Join-Path $resourceRoot 'assets\earthtitan\textures\entity\sword.png')
Save-Png (New-CoreLogo) (Join-Path $resourceRoot 'earthtitan_logo.png')

Write-Output 'Generated Earth Titan block, item, entity, sword, and logo PNGs.'
