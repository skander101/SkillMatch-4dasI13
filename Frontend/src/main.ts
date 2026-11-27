import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { bootstrapApplication } from '@angular/platform-browser';
import { App } from './app/app';
import { authInterceptor } from './app/user/auth.interceptor';

bootstrapApplication(App, {
  providers: [provideHttpClient(withInterceptors([authInterceptor]))]
}).catch((error) => console.error(error));
