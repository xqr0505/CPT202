# 秘钥配置说明

## 概述

JWT 秘钥是保证 Token 安全性的关键。不同环境需要不同的秘钥配置。

## 目录结构

```
application.yml           - 基础配置（公共）
application-dev.yml       - 开发环境配置（提交到 Git）
application-prod.yml      - 生产环境配置（不上传 Git）
application-local.yml     - 本地环境配置（不上传 Git）
```

## 秘钥生成方式

### 方式 1：使用随机字符串（推荐开发环境）

```
dev-secret-key-with-at-least-32-characters
```

### 方式 2：使用 Base64 编码的随机字节

```bash
# Linux/Mac
openssl rand -base64 32

# Windows PowerShell
[Convert]::ToBase64String((1..32 | ForEach-Object {Get-Random -Maximum 256}))

# 或使用 Python
python -c "import secrets; print(secrets.token_urlsafe(32))"
```

## 配置步骤

### Step 1: 生成秘钥

生成一个 32 字符以上的随机秘钥。例如：

```
a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0
```

### Step 2: 配置开发环境 (application-dev.yml)

```yaml
jwt:
  secret: your-dev-secret-key-here-32-characters-minimum
```

### Step 3: 配置本地环境 (application-local.yml)

在项目根目录创建 `application-local.yml`（不要提交到 Git）：

```yaml
jwt:
  secret: your-local-secret-key-here-32-characters-minimum
```

### Step 4: 配置生产环境 (application-prod.yml)

在生产环境中使用强秘钥：

```yaml
jwt:
  secret: your-production-secret-key-here-use-strong-random-32-characters-minimum
```

## Git 配置

### .gitignore

确保以下文件不被提交：

```
# application-local.yml 不提交到 Git
application-local.yml
```

## 使用方式

### 开发环境

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"
```

### 本地环境

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=local"
```

### 生产环境

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=prod"
```

## 安全建议

1. **绝不要在代码中硬编码秘钥**
2. **生产环境秘钥要足够随机和强大**
3. **定期轮换秘钥**
4. **将生产秘钥存储在安全的配置中心**（如 AWS Secrets Manager、Azure Key Vault）
5. **Team 沟通：** 通过团队安全通道分享秘钥，而不是通过 Git 或邮件

## 秘钥长度要求

- **最小：** 32 字符（256 位）
- **推荐：** 64 字符（512 位）

JWT 秘钥必须足够长来抵御暴力破解攻击。

## 调试

如果遇到 "Secret key must be at least 32 characters long" 错误：

1. 检查秘钥长度是否 >= 32 字符
2. 确认配置文件中的秘钥没有被截断
3. 检查 YAML 格式是否正确

## 参考

- [JJWT 文档](https://github.com/jwtk/jjwt)
- [JWT 最佳实践](https://tools.ietf.org/html/rfc8725)
