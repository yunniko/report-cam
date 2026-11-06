# Renders the Google Play store graphics from the launcher artwork (G-006 M3).
#   docs/play/icon-512.png            512 x 512, the adaptive icon's centre 72 dp on its background, as a launcher shows it
#   docs/play/feature-graphic.png     1024 x 500, the artwork with the app name and a tagline
# Draws with GDI+ (System.Drawing), so it needs no image tools beyond Windows. The shapes repeat
# app/src/main/res/drawable/ic_launcher_foreground.xml (108 dp viewport); keep the two in step.
#   pwsh scripts/play-graphics.ps1

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$out = Join-Path $PSScriptRoot '..\docs\play'
New-Item -ItemType Directory -Force $out | Out-Null

$blue = [System.Drawing.Color]::FromArgb(0x1F, 0x5F, 0x8B)
$orange = [System.Drawing.Color]::FromArgb(0xF2, 0xA3, 0x3A)
$white = [System.Drawing.Color]::White

# The three paths of ic_launcher_foreground.xml in viewport units; 4 dp corner radii as arcs.
function Add-Rounded([System.Drawing.Drawing2D.GraphicsPath]$p, [float]$x, [float]$y, [float]$w, [float]$h, [float]$r) {
    $d = 2 * $r
    $p.AddArc($x, $y, $d, $d, 180, 90)
    $p.AddArc($x + $w - $d, $y, $d, $d, 270, 90)
    $p.AddArc($x + $w - $d, $y + $h - $d, $d, $d, 0, 90)
    $p.AddArc($x, $y + $h - $d, $d, $d, 90, 90)
    $p.CloseFigure()
}

function Draw-Artwork([System.Drawing.Graphics]$g, [float]$scale, [float]$dx, [float]$dy) {
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.TranslateTransform($dx, $dy)
    $g.ScaleTransform($scale, $scale)
    # Camera body: a rounded rectangle 26..82 x 40..74 with a viewfinder bump 42..66 up to y=34.
    $body = [System.Drawing.Drawing2D.GraphicsPath]::new()
    Add-Rounded $body 26 40 56 34 4
    $bump = [System.Drawing.Drawing2D.GraphicsPath]::new()
    $bump.AddPolygon([System.Drawing.PointF[]]@(
        [System.Drawing.PointF]::new(42, 40.5), [System.Drawing.PointF]::new(46, 34),
        [System.Drawing.PointF]::new(62, 34), [System.Drawing.PointF]::new(66, 40.5)))
    $g.FillPath([System.Drawing.SolidBrush]::new($white), $body)
    $g.FillPath([System.Drawing.SolidBrush]::new($white), $bump)
    # Lens: circle centre (54,56) radius 10.
    $g.FillEllipse([System.Drawing.SolidBrush]::new($blue), 44, 46, 20, 20)
    # Stamp band: the body's bottom 8 dp, with the two lower corners rounded.
    $band = [System.Drawing.Drawing2D.GraphicsPath]::new()
    $band.AddLine(26, 66, 82, 66)
    $band.AddLine(82, 66, 82, 70)
    $band.AddArc(74, 66, 8, 8, 0, 90)
    $band.AddLine(78, 74, 30, 74)
    $band.AddArc(26, 66, 8, 8, 90, 90)
    $band.CloseFigure()
    $g.FillPath([System.Drawing.SolidBrush]::new($orange), $band)
    $g.ResetTransform()
}

# Icon: launchers show the centre 72 of the 108 dp viewport; Play masks the corners itself, so the square is full bleed.
$icon = [System.Drawing.Bitmap]::new(512, 512)
$g = [System.Drawing.Graphics]::FromImage($icon)
$g.Clear($blue)
$s = 512 / 72
Draw-Artwork $g $s (-18 * $s) (-18 * $s)
$g.Dispose()
$icon.Save((Join-Path $out 'icon-512.png'), [System.Drawing.Imaging.ImageFormat]::Png)

# Feature graphic: artwork on the left third, name and tagline on the right.
$fg = [System.Drawing.Bitmap]::new(1024, 500)
$g = [System.Drawing.Graphics]::FromImage($fg)
$g.Clear($blue)
$s = 420 / 72
Draw-Artwork $g $s (60 - 18 * $s) (40 - 18 * $s)
$g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit
$title = [System.Drawing.Font]::new('Segoe UI', 64, [System.Drawing.FontStyle]::Bold, [System.Drawing.GraphicsUnit]::Pixel)
$tag = [System.Drawing.Font]::new('Segoe UI', 30, [System.Drawing.FontStyle]::Regular, [System.Drawing.GraphicsUnit]::Pixel)
$g.DrawString('Report Cam', $title, [System.Drawing.SolidBrush]::new($white), 470, 160)
$g.DrawString("Date, place and note on the photo." + [Environment]::NewLine + 'Sent in one tap.', $tag, [System.Drawing.SolidBrush]::new($orange), 474, 250)
$g.Dispose()
$fg.Save((Join-Path $out 'feature-graphic.png'), [System.Drawing.Imaging.ImageFormat]::Png)

foreach ($f in 'icon-512.png', 'feature-graphic.png') {
    $b = [System.Drawing.Bitmap]::new((Join-Path $out $f))
    "$f $($b.Width)x$($b.Height) $((Get-Item (Join-Path $out $f)).Length) bytes"
    $b.Dispose()
}
