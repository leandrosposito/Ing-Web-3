import { describe, expect, it } from 'vitest'
import { formatearDuracion, formatearEta, formatearNumero } from './formato'

describe('formatearDuracion', () => {
  it('pasa segundos a hh:mm:ss', () => {
    expect(formatearDuracion(3725)).toBe('01:02:05')
    expect(formatearDuracion(0)).toBe('00:00:00')
  })
  it('sin dato devuelve un guion', () => {
    expect(formatearDuracion(null)).toBe('-')
  })
})

describe('formatearEta', () => {
  it('pasa horas a "h min"', () => {
    expect(formatearEta(1.5)).toBe('1 h 30 min')
    expect(formatearEta(0.25)).toBe('15 min')
    expect(formatearEta(0)).toBe('0 min')
  })
  it('sin dato devuelve un guion', () => {
    expect(formatearEta(undefined)).toBe('-')
  })
})

describe('formatearNumero', () => {
  it('redondea a los decimales pedidos', () => {
    expect(formatearNumero(3.14159)).toBe('3.1')
    expect(formatearNumero(3.14159, 3)).toBe('3.142')
    expect(formatearNumero(null)).toBe('-')
  })
})
