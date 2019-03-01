# ColorInversionService
定期的に色反転できるアプリ（誰得？）  
## 使い方（ちょっと特殊）  
用意するもの  
ADBが使えるパソコン  
1.コマンドプロンプト（PowerShell）（ターミナル）を開きます。  
2.Android端末のUSBデバックを有効にします（調べて）  
3.USBデバックにんしょうをする  
4.以下の文章を入れる  
>adb shell pm grant io.github.takusan23.colorinversionservice android.permission.WRITE_SECURE_SETTINGS  

5.なにも出なかったら成功。アプリを再起動しよう！
