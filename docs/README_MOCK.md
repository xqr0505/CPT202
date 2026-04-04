# Mock 登录与角色切换说明

本项目当前处于 Mock 阶段，前端已写死登录状态和用户信息，方便开发和联调。你可以通过修改 `frontend/src/stores/user.ts` 文件，快速切换不同的用户角色。

## 如何切换测试用户角色

1. 打开文件：
   ```
   frontend/src/stores/user.ts
   ```

2. 找到如下代码片段（大约在 20 行附近）：
   ```typescript
   // FIXME: 强行塞入假 token
   const token = ref<string | null>('fake-jwt-token-12345')
   
   // FIXME: 强行写死一个用户信息
   const userInfo = ref<UserInfo | null>({
     id: 1,
     username: 'mockuser',
     nickname: 'Mock Customer'
   })
   const userRole = ref<UserRole>(USER_ROLES.CUSTOMER)
   ```

3. 修改 `userRole` 的值即可切换角色：
   - 普通用户：`USER_ROLES.CUSTOMER`
   - 专家：`USER_ROLES.SPECIALIST`
   - 管理员：`USER_ROLES.ADMIN`

   例如：
   ```typescript
   const userRole = ref<UserRole>(USER_ROLES.SPECIALIST)
   ```

4. 如需更换用户信息（如昵称、id），直接修改 `userInfo` 对象。

5. 保存文件，刷新前端页面即可生效。

## 注意事项
- 当前所有页面都默认已登录，不会跳转到登录页。
- 后端也已放行所有接口，无需真实鉴权。
- Mock 阶段仅用于开发联调，勿用于生产环境。

---
如需恢复真实登录鉴权，请将 `user.ts` 和 `router/permission.ts` 中的相关 FIXME 代码恢复原状。

