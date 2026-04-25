import { createSlice, PayloadAction } from '@reduxjs/toolkit';
import { Claim, ClaimFilter } from '@/types';

interface ClaimsState {
  claims: Claim[];
  selectedClaim: Claim | null;
  loading: boolean;
  error?: string;
  filter: ClaimFilter;
  pagination: {
    page: number;
    limit: number;
    total: number;
  };
}

const initialState: ClaimsState = {
  claims: [],
  selectedClaim: null,
  loading: false,
  filter: {},
  pagination: {
    page: 1,
    limit: 10,
    total: 0,
  },
};

const claimsSlice = createSlice({
  name: 'claims',
  initialState,
  reducers: {
    setLoading: (state, action: PayloadAction<boolean>) => {
      state.loading = action.payload;
    },
    setClaims: (state, action: PayloadAction<Claim[]>) => {
      state.claims = action.payload;
      state.loading = false;
    },
    setSelectedClaim: (state, action: PayloadAction<Claim | null>) => {
      state.selectedClaim = action.payload;
    },
    addClaim: (state, action: PayloadAction<Claim>) => {
      state.claims.unshift(action.payload);
    },
    updateClaim: (state, action: PayloadAction<Claim>) => {
      const index = state.claims.findIndex((c) => c.claimId === action.payload.claimId);
      if (index !== -1) {
        state.claims[index] = action.payload;
      }
      if (state.selectedClaim?.claimId === action.payload.claimId) {
        state.selectedClaim = action.payload;
      }
    },
    removeClaim: (state, action: PayloadAction<string>) => {
      state.claims = state.claims.filter((c) => c.claimId !== action.payload);
    },
    setFilter: (state, action: PayloadAction<ClaimFilter>) => {
      state.filter = action.payload;
      state.pagination.page = 1;
    },
    setPagination: (
      state,
      action: PayloadAction<{ page: number; limit: number; total: number }>
    ) => {
      state.pagination = action.payload;
    },
    setError: (state, action: PayloadAction<string>) => {
      state.error = action.payload;
      state.loading = false;
    },
    clearError: (state) => {
      state.error = undefined;
    },
  },
});

export const {
  setLoading,
  setClaims,
  setSelectedClaim,
  addClaim,
  updateClaim,
  removeClaim,
  setFilter,
  setPagination,
  setError,
  clearError,
} = claimsSlice.actions;
export default claimsSlice.reducer;
