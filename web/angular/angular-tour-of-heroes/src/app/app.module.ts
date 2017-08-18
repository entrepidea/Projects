import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import {FormsModule} from '@angular/forms';
import {RouterModule} from '@angular/router';

import {AppRoutingModule} from './app-routing.module';

import { AppComponent } from './app.component';
import {HeroesComponent} from './heroes.component';
import { HeroDetailComponent } from './hero-detail.component';
import {DashboardComponent} from './dashboard.component';



import {HeroService} from './hero.service';


@NgModule({
  declarations: [
    AppComponent,
    DashboardComponent,
    HeroDetailComponent,
    HeroesComponent
  ],
  imports: [
    BrowserModule,
    FormsModule,
    AppRoutingModule
  ],
  providers: [HeroService],
  bootstrap: [AppComponent]
})
export class AppModule { }