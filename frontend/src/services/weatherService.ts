import { apiClient } from './apiClient';
import type { WeatherInfoDto, MarineSafetySummary } from '../types/weather';

const MAN_THAI_LAT = 16.089035780716284;
const MAN_THAI_LNG = 108.24959555394304;

// Cache memory to prevent excessive API calls on navigation (5-minute TTL)
let cachedWeather: WeatherInfoDto | null = null;
let cacheTimestamp = 0;
const CACHE_TTL_MS = 5 * 60 * 1000;

export const weatherService = {
  /**
   * Fetch current live weather and marine condition at Danang Coast (Son Tra / Man Thai)
   */
  async getCurrentWeather(
    lat: number = MAN_THAI_LAT,
    lng: number = MAN_THAI_LNG,
    forceRefresh: boolean = false
  ): Promise<WeatherInfoDto> {
    const now = Date.now();
    if (!forceRefresh && cachedWeather && now - cacheTimestamp < CACHE_TTL_MS) {
      console.log('[weatherService] Trả về dữ liệu thời tiết biển từ in-memory cache:', cachedWeather);
      return cachedWeather;
    }

    console.groupCollapsed('%c[Weather API] Lấy dữ liệu thời tiết biển thời gian thực...', 'color: #00646f; font-weight: bold;');
    console.log('[Bước 1/3] Gửi yêu cầu GET /api/weather/current với tọa độ:', { lat, lng });

    try {
      const response = await apiClient.get<WeatherInfoDto>('/weather/current', {
        params: { lat, lng },
      });

      console.log('[Bước 2/3] Nhận dữ liệu thành công từ Backend:', response.data);
      cachedWeather = response.data;
      cacheTimestamp = now;

      console.log('[Bước 3/3] Cache dữ liệu thành công (TTL: 5 phút)');
      console.groupEnd();
      return response.data;
    } catch (err) {
      console.groupEnd();
      console.error('[weatherService] Lỗi khi tải dữ liệu thời tiết biển:', err);
      throw err;
    }
  },

  /**
   * Evaluate marine safety level matching Backend WeatherRuleEngine baseline
   */
  evaluateSafety(data: WeatherInfoDto): MarineSafetySummary {
    const wave = data.marine?.waveHeight ?? 0;
    const wind = data.weather?.windSpeed ?? 0;
    const gust = data.weather?.windGust ?? 0;
    const code = data.weather?.weatherCode ?? 0;

    // Severe thunderstorm WMO codes (95, 96, 99)
    const isFatalThunderstorm = [95, 96, 99].includes(code);

    if (isFatalThunderstorm || wave > 1.2 || wind > 28 || gust > 38) {
      return {
        level: 'RED',
        statusLabel: 'Biển động',
        statusLabelEn: 'Rough Sea',
      };
    }

    if (wave > 0.7 || wind > 18) {
      return {
        level: 'YELLOW',
        statusLabel: 'Cần lưu ý',
        statusLabelEn: 'Caution',
      };
    }

    return {
      level: 'GREEN',
      statusLabel: 'Biển êm',
      statusLabelEn: 'Calm Sea',
    };
  },
};
