# SteadyLAN Setting SDK Android Java

This SDK using StarIO library is for changing SteadyLAN setting.

## What is SteadyLAN
- [SteadyLAN (English)](https://www.star-m.jp/products/s_print/mcprint3/manual/en/settings/SteadyLAN.htm)
- [SteadyLAN (Japanese)](https://www.star-m.jp/products/s_print/mcprint3/manual/ja/settings/SteadyLAN.htm)                      

## Supported models
- mC-Print2 (Printer firmware version 2.3 or later) without MCP20 and MCP20B
- mC-Print3 (Printer firmware version 2.3 or later) without MCP30

## Requirements
- SteadyLAN for iOS: iOS 10.1.1 or later
- SteadyLAN for Android: Android 5.0 or later
- SteadyLAN for Windows: <u>Windows 10 or later</u>

## Reference
### Online Manual
- [mC-Print2 Online Manual](http://www.star-m.jp/mcprint2-oml.html)
- [mC-Print3 Online Manual](http://www.star-m.jp/mcprint3-oml.html)

### StarPRNT Command Specifications
- [StarPRNT Command Specifications (English)](https://www.starmicronics.com/support/Mannualfolder/StarPRNT_cm_en.pdf)
- [StarPRNT Command Specifications (Japanese)](http://sp-support.star-m.jp/Mannualfolder/starprnt_cm_jp.pdf)

## Note
- SteadyLAN function must be configured to match the operating system (OS) of the device. It cannot be used with an OS different from the printer's settings.
- A printer where the SteadyLAN function is set to “Enable for Windows” cannot perform USB communication with Android devices. In this case, either connect a device with a different OS, or else connect to the printer from a different interface, then either set the printer SteadyLAN function to “Disable” or “Enable for Android”. Or, initialize the communication settings, but when doing it, be aware that other settings will also be returned to the default settings from the time when the product was purchased.
- SteadyLAN function is not available from the UWP application even if you set it to SteadyLAN for Windows because UWP application cannot communicate with the Star Printer via USB I/F.
This function is available for [Windows Desktop applications](https://github.com/star-micronics/SteadyLAN-Setting-SDK-WindowsDesktop-Labs) when you set to SteadyLAN for Windows.
- Parameters for specifying SteadyLAN settings may or may not be supported depending on the printer model.
The support relationship is as shown in the table below. If not supported, the command is ignored.

| Parameter(n)<br>HEX | SteadyLAN | Model<br>MCP31L/MCP31LB | <br>MCP31C/MCP31CB | <br>MCP30 | <br>MCP21L | <br>MCP20/MCP20B |
| ---- | ---- | ---- | ---- | ---- | ---- | ---- |
| 0x00 | Invalid | <div style="text-align: center; color: lightgreen;">✔</div> | <div style="text-align: center; color: lightgreen;">✔</div> | <div style="text-align: center; color: red;">✘</div> | <div style="text-align: center; color: lightgreen;">✔</div> | <div style="text-align: center; color: red;">✘</div> |
| 0x01 | Valid (for iOS) | <div style="text-align: center; color: lightgreen;">✔</div> | <div style="text-align: center; color: red;">✘</div> | <div style="text-align: center; color: red;">✘</div> | <div style="text-align: center; color: lightgreen;">✔</div> | <div style="text-align: center; color: red;">✘</div> |
| 0x02 | Valid (for Android) | <div style="text-align: center; color: red;">✘</div> | <div style="text-align: center; color: lightgreen;">✔</div> | <div style="text-align: center; color: red;">✘</div> | <div style="text-align: center; color: red;">✘</div> | <div style="text-align: center; color: red;">✘</div> |
| 0x03 | Valid (for Windows) | <div style="text-align: center; color: red;">✘</div> | <div style="text-align: center; color: lightgreen;">✔</div> | <div style="text-align: center; color: red;">✘</div> | <div style="text-align: center; color: red;">✘</div> | <div style="text-align: center; color: red;">✘</div> |

## Copyright

Copyright 2020 Star Micronics Co., Ltd. All rights reserved.