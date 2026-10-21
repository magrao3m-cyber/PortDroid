# PortZone Android - Central de Jogos & Ports (485+ Jogos)

Aplicativo Android moderno construído em **Kotlin** e **Jetpack Compose** com a estética **Cyber-Minimalism**. O app permite gerenciar, baixar e instalar fan ports e source ports para Android, além de sincronizar com o Google Drive e gerenciar arquivos de dados complementares (.WAD, .PAK, .Z64, .ZIP) via **Storage Access Framework (SAF)**.

---

## 🌟 Funcionalidades Principais

1. **Design System Cyber-Minimalism:**
   - Paleta OLED profunda (`#0B0C10`, `#1F2833`) com toques em verde neon (`#00E676`) e ciano elétrico (`#00E5FF`).
   - Abas em formato de pílulas deslizantes dinâmicas por categoria de jogo.
   - Cards com miniaturas de alta qualidade, tags de versão e botão interativo com transição de estados animados.

2. **Downloads Nativos em Segundo Plano (`DownloadManager`):**
   - Downloads assíncronos que continuam mesmo se o aplicativo for fechado.
   - Notificações nativas com barra de progresso em tempo real.
   - Abertura imediata do instalador de pacotes do Android via `FileProvider`.

3. **Limpeza Automática Pós-Instalação:**
   - Detecta no ciclo de vida `onResume` quando um jogo acabou de ser instalado no sistema.
   - Exclui o arquivo instalador `.apk` antigo da pasta de downloads para economizar armazenamento.

4. **Gerenciador de Dados e ROMs via SAF (Storage Access Framework):**
   - Suporte automatizado a arquivos complementares: `.wad` (Doom), `.pak`/`.pk3` (Quake/RTCW), `.z64` (Super Mario 64), `.grp` (Duke Nukem 3D), `.mpq` (Diablo), `.dat` (Fallout), `.zip`/`.7z`.
   - Seletor de pastas de emuladores e ports com permissão persistente (`ACTION_OPEN_DOCUMENT_TREE`).
   - Cópia ou descompactação automática de archives diretamente para a pasta do jogo sem bloqueios de Scoped Storage.

5. **Segurança Avançada com Hardware Keystore:**
   - Criptografia AES-256 baseada em hardware via **AndroidX Security Crypto (Tink)**.
   - As credenciais da Conta de Serviço do Google Drive são descriptografadas diretamente na memória RAM, sem salvar arquivos em texto puro no disco.
   - Regras ProGuard/R8 ativadas contra engenharia reversa.

---

## 🛠️ Como Compilar e Rodar

### Requisitos:
* **JDK 17+**
* **Android SDK 34**

### Compilação via Linha de Comando:
```bash
./gradlew assembleDebug
```
O APK final será gerado em:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 📤 Publicando no seu Repositório GitHub

Para enviar o código para o seu repositório no GitHub:

```bash
# 1. Conecte ao seu repositório remoto
git remote add origin https://github.com/SEU_USUARIO/SEU_REPOSITORIO.git

# 2. Defina a branch principal
git branch -M main

# 3. Envie os arquivos
git push -u origin main
```
*(Nota: O arquivo `credentials.json` sensível já está configurado no `.gitignore` para proteção das suas chaves de API).*
