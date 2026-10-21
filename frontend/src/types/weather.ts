export interface WeatherData {
  sourceFetchedAt?: string;
  time?: string;
  temperature: number;
  precipitation?: number;
  windSpeed: number;
  windGust?: number;
  windDirection?: number;
  visibility?: number;
  cloudCover?: number;
  weatherCode?: number;
  precipitationProbability?: number;
  uvIndex?: number;
  relativeHumidity?: number;
  dewPoint?: number;
  surfacePressure?: number;
}

export interface MarineData {
  sourceFetchedAt?: string;
  time?: string;
  waveHeight: number;
  waveDirection?: number;
  wavePeriod?: number;
  swellHeight?: number;
  swellDirection?: number;
  swellPeriod?: number;
  oceanCurrentVelocity?: number;
  oceanCurrentDirection?: number;
  seaLevelHeight?: number;
  seaSurfaceTemperature?: number;
}

export interface WeatherInfoDto {
  latitude: number;
  longitude: number;
  weather: WeatherData;
  marine?: MarineData;
  fetchedAt?: string;
  source?: string;
  stale?: boolean;
  dataCoverage?: string;
  estimatedMarine?: boolean;
}

export type MarineSafetyLevel = 'GREEN' | 'YELLOW' | 'RED';

export interface MarineSafetySummary {
  level: MarineSafetyLevel;
  statusLabel: string;
  statusLabelEn: string;
}
