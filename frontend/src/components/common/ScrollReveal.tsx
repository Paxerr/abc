import React, { useEffect, useRef, useState } from 'react';

interface ScrollRevealProps {
  children: React.ReactNode;
  delay?: number;
  className?: string;
  duration?: number;
}

const prefersReducedMotion = (): boolean =>
  typeof window !== 'undefined' && window.matchMedia('(prefers-reduced-motion: reduce)').matches;

export const ScrollReveal: React.FC<ScrollRevealProps> = ({
  children,
  delay = 0,
  duration = 700,
  className = '',
}) => {
  const ref = useRef<HTMLDivElement>(null);
  const [reducedMotion] = useState(prefersReducedMotion);
  const [isVisible, setIsVisible] = useState(reducedMotion);
  const [isRendered, setIsRendered] = useState(reducedMotion);

  useEffect(() => {
    if (reducedMotion) return;

    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          setIsVisible(true);
          observer.disconnect();

          // Release will-change and transform after animation completes
          const timer = setTimeout(() => {
            setIsRendered(true);
          }, duration + delay + 100);

          return () => clearTimeout(timer);
        }
      },
      {
        threshold: 0.01,
        rootMargin: '0px 0px 80px 0px',
      }
    );

    if (ref.current) {
      observer.observe(ref.current);
    }

    return () => observer.disconnect();
  }, [delay, duration, reducedMotion]);

  // Clean styles once animation has completed
  if (isRendered) {
    return (
      <div ref={ref} className={className}>
        {children}
      </div>
    );
  }

  return (
    <div
      ref={ref}
      className={className}
      style={{
        opacity: isVisible ? 1 : 0,
        transform: isVisible ? 'translate3d(0, 0, 0)' : 'translate3d(0, 20px, 0)',
        transition: `opacity ${duration}ms cubic-bezier(0.16, 1, 0.3, 1) ${delay}ms, transform ${duration}ms cubic-bezier(0.16, 1, 0.3, 1) ${delay}ms`,
        willChange: isVisible ? 'opacity, transform' : 'auto',
      }}
    >
      {children}
    </div>
  );
};

export default ScrollReveal;
