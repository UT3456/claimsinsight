import { useDispatch, useSelector } from 'react-redux';
import { RootState, AppDispatch } from '@/store';

export const useAppDispatch = () => useDispatch<AppDispatch>();
export const useAppSelector = <T,>(selector: (state: RootState) => T) => useSelector(selector);

export const useAuth = () => {
  const auth = useAppSelector((state) => state.auth);
  const dispatch = useAppDispatch();

  const isAuthenticated = auth.isAuthenticated || !!localStorage.getItem('authToken');
  const user = auth.user || (localStorage.getItem('user') ? JSON.parse(localStorage.getItem('user')!) : null);
  const loading = auth.loading;
  const error = auth.error;

  return {
    isAuthenticated,
    user,
    loading,
    error,
    dispatch,
  };
};

export const useClaims = () => {
  const claims = useAppSelector((state) => state.claims);
  const dispatch = useAppDispatch();

  return {
    claims: claims.claims,
    selectedClaim: claims.selectedClaim,
    loading: claims.loading,
    error: claims.error,
    filter: claims.filter,
    pagination: claims.pagination,
    dispatch,
  };
};
