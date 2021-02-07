import { customElement, html, LitElement, css, property } from 'lit-element';
import '@vaadin/vaadin-icons/vaadin-icons';
import '@vaadin/vaadin-ordered-layout';
import AppInfo from '../../generated/com/github/mcollovati/vertxvaadin/fusion/data/endpoint/InfoEndpoint/AppInfo';
import * as InfoEndpoint from '../../generated/InfoEndpoint';

@customElement('about-view')
export class AboutView extends LitElement {
  static get styles() {
    return css`
      :host {
        display: block;
      }
    `;
  }

  @property()
  private info: AppInfo = {
      vaadinVersion: "",
      vertxVaadinVersion: "",
      vertxVersion: ""
  }

  render() {
    return html`<vaadin-vertical-layout theme="padding spacing">
        <div>This application is using
            <b><iron-icon icon="vaadin:vaadin-v"></iron-icon>
            <a href="http://vaadin.com/">Vaadin Flow (Fusion)</a> ${this.info.vaadinVersion}</b></div>
        <div>running on top of <b><a href="http://vertx.io/">Vert.x</a> ${this.info.vertxVersion}</b></div>
        <div>using <b><a href="https://github.com/mcollovati/vertx-vaadin">Vertx-Vaadin-Flow</a> ${this.info.vertxVaadinVersion}</b></div>
    </vaadin-vertical-layout>`;
  }

  async connectedCallback() {
    super.connectedCallback();
    this.info = await InfoEndpoint.info();
    console.log("======================= this.info", this.info)
  }

}
