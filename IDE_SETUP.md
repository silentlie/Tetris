# IDE Setup – IntelliJ IDEA

This guide helps you configure IntelliJ IDEA for consistent development practices on this Tetris project.

---

## 1. Auto-Save and Format on Save

1. Go to **⚙️ > Settings > Appearance & Behavior > System Settings**
    - Enable **"Save files automatically if the application is idle"**
2. Go to **⚙️ > Settings️ > Tools > Actions on Save**
    - Enable **"Reformat code"**
    - Enable **"Optimize imports"**
    - Enable **"Rearrange code"**
    - Enable **"Run code cleanup"**

---

## 2. Install CheckStyle Plugin

1. Click the gear icon **⚙️ > Plugins**
2. Search for `CheckStyle-IDEA` and install it.
3. Restart IntelliJ after installation.

---

## Configure CheckStyle with Google Style

1. Go to **⚙️ > Settings > Tools > Checkstyle**
2. Enable **Google Checks**
3. Run CheckStyle: **Right-click file > Check Current File**
4. Or Bottom-Left: **Click CheckStyle > Check Current File/Check Project/...**

---

## Add Google Java Code Style

### Option A: Manually import XML

1. Download [google-java-format.xml](https://github.com/google/styleguide/blob/gh-pages/intellij-java-google-style.xml)
2. Go to **⚙️ > Settings > Editor > Code Style > Java**
3. Click the gear icon **⚙️ > Import Scheme > IntelliJ IDEA code style XML**
4. Select the downloaded file.

### Option B: Use Google's plugin

Install [google-java-format plugin](https://plugins.jetbrains.com/plugin/8527-google-java-format) for IntelliJ if preferred.

---

## Optional: Install JavaDoc Plugin (Recommended)

1. Click the gear icon **⚙️ > Plugins**
2. Search for `JavaDoc` and install it.
3. Restart IntelliJ after installation.

### Using JavaDoc

- Place your cursor on a class, method, or field
- Press `Alt + Enter` and select **"Add Javadoc"**

---

## Optional: Enable Format Check on Commit

1. Go to **⚙️ > Settings > Version Control > Commit**
2. Enable:
    - **Perform code analysis**
    - **Reformat code**
    - **Optimize imports**

---