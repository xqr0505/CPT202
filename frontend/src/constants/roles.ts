/**
 * Constants related to user roles
 */
export const USER_ROLES = {
  CUSTOMER: 'customer',
  SPECIALIST: 'specialist',
  ADMIN: 'admin'
} as const;

export type UserRoleType = typeof USER_ROLES[keyof typeof USER_ROLES];

