Add-Type -AssemblyName System.Speech
$s = New-Object System.Speech.Synthesis.SpeechSynthesizer
$s.SetOutputToWaveFile("C:\Users\huangshaozheng\WorkBuddy\2026-09-20-14-20-48\anker-hackathon\scripts\tts_test_zh.wav")
$s.Speak("大家好，今天我想分享一个关于语音识别的测试，这个灵感来自通勤路上的思考")
$s.Dispose()
Write-Output "TTS-OK"
