export interface State {
  id: number;
  code: string;
  name: string;
  created_at: Date;
}

export interface City {
  id: number;
  state_id: number;
  name: string;
  normalized_name: string;
  latitude: number;
  longitude: number;
  created_at: Date;
}

export interface Category {
  id: number;
  name: string;
  type: 'service' | 'product';
  icon_url?: string;
  created_at: Date;
}
