import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { bootstrapApplication } from '@angular/platform-browser';
import { provideRouter, Routes } from '@angular/router';
import { App } from './app/app';
import { MessagerieComponent } from './app/messagerie/messagerie';
import { authInterceptor } from './app/user/auth.interceptor';

const routes: Routes = [
  {
    path: '',
    children: [
      { path: 'messagerie', component: MessagerieComponent }
    ]
  },
  { path: '**', redirectTo: '' }
];

bootstrapApplication(App, {
  providers: [
    provideHttpClient(withInterceptors([authInterceptor])),
    provideRouter(routes)
  ]
}).catch((error) => console.error(error));
