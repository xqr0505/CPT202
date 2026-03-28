import { USER_ROLES } from '@/constants/roles';

// TODO: Implement user data fetching logic and assign the correct role here.
export const fetchUserProfile = async () => {
  // Placeholder implementation
  return Promise.resolve({
    id: 1,
    username: 'test_user',
    nickname: 'Test User',
    role: USER_ROLES.CUSTOMER // USER_ROLES.SPECIALIST / USER_ROLES.ADMIN / USER_ROLES.CUSTOMER
  });
};
